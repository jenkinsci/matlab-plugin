import jenkins
import time
import requests
import sys
import os

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

def wait_for_completion(server, job_name, build_number, timeout_mins=10):
    print(f"   [ACTION] Waiting for Build #{build_number}")
    start_time = time.time()
    
    timeout_seconds = timeout_mins * 60
    
    while (time.time() - start_time) < timeout_seconds:
        try:
            info = server.get_build_info(job_name, build_number)
            if not info['building']: 
                return info['result']
        except: 
            pass
        time.sleep(10)
    
    print(f"   [ERROR] Timeout: Build #{build_number} did not complete within {timeout_mins} minutes.")
    return "TIMEOUT"

def print_console_output(server, job_name, build_number):
    print(f"   [ACTION] Printing Console Output...")
    try:
        console_output = server.get_build_console_output(job_name, build_number)
        print(console_output)
    except Exception as e:
        print(f"      -> [ERROR] {e}")

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
            print("      -> Build and artifacts deleted successfully.")
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
        info = server.get_build_info(job_name, build_number)
        node_name = info.get('builtOn', '')
        
        display_node = node_name if node_name else "master"
        print(f"   [CLEANUP] Removing MATLAB installation from {display_node}...")

        groovy_script = f"""
        import jenkins.model.*
        import hudson.FilePath
        
        def nodeName = "{node_name}"
        def node = (nodeName == "") ? Jenkins.instance : Jenkins.instance.getNode(nodeName)

        if (node == null) {{
            println "ERROR: Node '" + nodeName + "' no longer exists."
            return
        }}

        def rootPath = node.getRootPath()
        if (rootPath == null) {{
            println "ERROR: Node is offline."
            return
        }}

        def toolDir = rootPath.child("tools/com.mathworks.ci.MatlabInstallation")
        
        if (toolDir.exists()) {{
            try {{
                toolDir.deleteRecursive()
                println "SUCCESS: Deleted MATLAB installation on " + node.getDisplayName()
            }} catch (Exception e) {{
               println "ERROR: Failed to delete MATLAB installation directory at " + toolDir.getRemote() + ". Error: " + e.getMessage()
            }}
        }} else {{
            println "SKIP: Directory not found (already clean)."
        }}
        """
        
        output = server.run_script(groovy_script)
        print(f"      -> {output.strip()}")

    except Exception as e:
        print(f"      -> [WARNING] Remote cleanup failed: {e}")