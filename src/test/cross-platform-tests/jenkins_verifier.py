"""
Cross-Platform Test
===========================
Usage:
    python jenkins_verifier.py --url <URL> --user <USER> --token <TOKEN> --job <JOB_NAME>
"""

import jenkins
import time
import argparse
import sys
import requests
import os

# --- CONSTANTS ---
ARTIFACTS_TO_VERIFY = [
    {"name": "JUnit XML", "filename": "junittestresults.xml", "expected_content": "testAddition"},
    {"name": "PDF Report", "filename": "testreport.pdf", "expected_content": "%PDF"},
    {"name": "TAP Results", "filename": "taptestresults.tap", "expected_content": "TAP version 13"},
    {"name": "Cobertura XML", "filename": "cobertura.xml", "expected_content": "coverage"}
]

# --- HELPER FUNCTIONS ---

def trigger_build(server, job_name):
    print(f"   [ACTION] Triggering build...")
    return server.build_job(job_name)

def get_build_number(server, queue_id):
    start = time.time()
    while time.time() - start < 60:
        try:
            q = server.get_queue_item(queue_id)
            if 'executable' in q: return q['executable']['number']
            if q.get('cancelled'): return None
        except: pass
        time.sleep(2)
    return None

def wait_for_completion(server, job_name, build_number):
    print(f"   [ACTION] Waiting for Build #{build_number}...")
    while True:
        try:
            info = server.get_build_info(job_name, build_number)
            if not info['building']: return info['result']
        except: pass
        time.sleep(5)

def verify_artifact(server, base_url, auth, job_name, build_number, case):
    print(f"   [TEST] Verifying Artifact: {case['name']}")
    try:
        info = server.get_build_info(job_name, build_number)
        artifacts = info.get('artifacts', [])
        found = next((a for a in artifacts if case['filename'] in a['fileName']), None)
        
        if not found:
            print(f"      -> [FAIL] Artifact '{case['filename']}' not found in Jenkins archive.")
            return False
            
        url = f"{base_url.rstrip('/')}/job/{job_name}/{build_number}/artifact/{found['relativePath']}"
        res = requests.get(url, auth=auth)
        
        snippet = res.content[:100].decode('utf-8', errors='ignore')
        if case['expected_content'] in snippet or case['expected_content'] in res.text:
            print(f"      -> [PASS] Content verified.")
            return True
        else:
            print(f"      -> [FAIL] Content mismatch. Expected: {case['expected_content']}")
            return False
    except Exception as e:
        print(f"      -> [ERROR] {e}")
        return False

def cleanup_build(base_url, auth, job_name, build_number):
    """Deletes the build record."""
    if not build_number:
        return
        
    print(f"   [CLEANUP] Deleting Build #{build_number}...")
    try:
        base = base_url.rstrip('/')
        delete_url = f"{base}/job/{job_name}/{build_number}/doDelete"
        res = requests.post(delete_url, auth=auth)
        
        if res.status_code in [200, 204, 302]:
            print("      -> Build deleted successfully.")
        else:
            print(f"      -> [WARNING] Failed to delete build. Status: {res.status_code}")
            
    except Exception as e:
        print(f"      -> [WARNING] Failed to delete build: {e}")

def delete_matlab_tools(server, job_name, build_number):
    """
    Identifies which node ran the build and performs a REMOTE delete
    of the MPM-installed MATLAB directory on that specific node.
    """
    try:
        # 1. Get Build Info to find out WHICH node ran the build
        info = server.get_build_info(job_name, build_number)
        node_name = info.get('builtOn', '')  # Empty string means 'built on master'
        
        # Use specific machine name for logging
        display_node = node_name if node_name else "master"
        print(f"   [CLEANUP] Removing MATLAB installation from {display_node}...")

        # 2. Groovy Script using FilePath (Works for both Master and Remote Slaves)
        groovy_script = f"""
        import jenkins.model.*
        import hudson.FilePath
        
        def nodeName = "{node_name}"
        def node = null
        
        if (nodeName == "") {{
            node = Jenkins.instance
        }} else {{
            node = Jenkins.instance.getNode(nodeName)
        }}

        if (node == null) {{
            println "ERROR: Node '" + nodeName + "' no longer exists."
            return
        }}

        def rootPath = node.getRootPath()
        if (rootPath == null) {{
            println "ERROR: Node is offline."
            return
        }}

        // Target the specific MPM installation folder
        def toolDir = rootPath.child("tools/com.mathworks.ci.MatlabInstallation")
        
        if (toolDir.exists()) {{
            try {{
                toolDir.deleteRecursive()
                println "SUCCESS: Deleted MATLAB tools on " + node.getDisplayName()
            }} catch (Exception e) {{
                println "ERROR: Failed to delete. " + e.getMessage()
            }}
        }} else {{
            println "SKIP: Directory not found on " + node.getDisplayName() + " (already clean)."
        }}
        """
        
        output = server.run_script(groovy_script)
        print(f"      -> {output.strip()}")

    except Exception as e:
        print(f"      -> [WARNING] Remote cleanup failed: {e}")


# --- LOGIC SEPARATION ---

def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--user", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--job", required=True)
    return parser.parse_args()

def run_test_suite(args):
    server = jenkins.Jenkins(args.url, username=args.user, password=args.token)
    build_num = None

    print("=== STARTING TEST SUITE ===")
    
    try:
        # 1. Trigger Build
        queue_id = trigger_build(server, args.job)
        build_num = get_build_number(server, queue_id)
        
        if not build_num: 
            print("   [FATAL] Could not get build number.")
            return False
        
        # 2. Wait for Finish
        result = wait_for_completion(server, args.job, build_num)
        if result != 'SUCCESS':
            print(f"   [FATAL] Build failed with status: {result}")
            return False

        # 3. Verify Artifacts
        failed = []
        for artifact in ARTIFACTS_TO_VERIFY:
            if not verify_artifact(server, args.url, (args.user, args.token), args.job, build_num, artifact):
                failed.append(artifact['name'])

        print("\n" + "="*40)
        if failed:
            print(f"FAILURES: {failed}")
            return False
        else:
            print("SUCCESS: All artifacts verified successfully.")
            return True

    except KeyboardInterrupt:
        print("\n   [ABORTED] Execution stopped by user.")
        return False
    except Exception as e:
        print(f"   [CRITICAL ERROR] {e}")
        return False
    finally:
        # 4. Cleanup Phase (ORDER CHANGED: Tool cleanup FIRST, Build record SECOND)
        print("\n" + "-"*20)
        
        if build_num:
            # First: Clean the tools (needs build info to find the node)
            delete_matlab_tools(server, args.job, build_num)
            
            # Second: Delete the build record
            cleanup_build(args.url, (args.user, args.token), args.job, build_num)

# ==========================================
# MAIN EXECUTION
# ==========================================
def main():
    args = parse_arguments()
    success = run_test_suite(args)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()