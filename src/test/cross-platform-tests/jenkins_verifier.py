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
import xml.etree.ElementTree as ET

ARTIFACTS_TO_VERIFY = [
    {"name": "JUnit XML", "filename": "junittestresults.xml", "expected_content": "testAddition"},
    {"name": "PDF Report", "filename": "testreport.pdf", "expected_content": "%PDF"},
    {"name": "TAP Results", "filename": "taptestresults.tap", "expected_content": "TAP version 13"},
    {"name": "Cobertura XML", "filename": "cobertura.xml", "expected_content": "coverage"}
]

JENKINS_XML_MAP = {
    "junitArtifact": ("JunitArtifact", "matlabTestArtifacts/junittestresults.xml"),
    "pdfReportArtifact": ("PdfArtifact", "matlabTestArtifacts/testreport.pdf"),
    "tapArtifact": ("TapArtifact", "matlabTestArtifacts/taptestresults.tap"),
    "coberturaArtifact": ("CoberturaArtifact", "matlabTestArtifacts/cobertura.xml")
}

def configure_freestyle_job(server, job_name):
    print(f"   [CONFIG] Configuring job '{job_name}' to generate all artifacts...")
    config_xml = server.get_job_config(job_name)
    ET.register_namespace("", "")
    root = ET.fromstring(config_xml)
    
    base_class_package = "com.mathworks.ci.freestyle.RunMatlabTestsBuilder$"
    changes = 0

    for tag_name, (class_suffix, file_path) in JENKINS_XML_MAP.items():
        for elem in root.iter(tag_name):
            new_class = f"{base_class_package}{class_suffix}"
            current_class = elem.get('class', '')
            
            current_xml_str = ET.tostring(elem, encoding='unicode')
            path_needs_update = file_path not in current_xml_str
            
            if current_class != new_class or path_needs_update:
                elem.set('class', new_class)
                for child in list(elem): elem.remove(child) 
                
                elem.text = None
                path_elem = ET.Element("filePath")
                path_elem.text = file_path
                elem.append(path_elem)
                changes += 1

    for archiver in root.iter("hudson.tasks.ArtifactArchiver"):
        for artifacts_tag in archiver.iter("artifacts"):
            if artifacts_tag.text != "matlabTestArtifacts/**/*":
                artifacts_tag.text = "matlabTestArtifacts/**/*"
                changes += 1

    if changes > 0:
        new_config_str = ET.tostring(root, encoding='utf-8', method='xml').decode('utf-8')
        server.reconfig_job(job_name, new_config_str)
        print("   [CONFIG] Job reconfigured successfully.")
    else:
        print("   [CONFIG] Job configuration was already correct.")

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

def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--user", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--job", required=True)
    return parser.parse_args()

def run_test_suite(args):
    try:
        server = jenkins.Jenkins(args.url, username=args.user, password=args.token)
        
        print("=== STARTING TEST SUITE ===")
        
        configure_freestyle_job(server, args.job)
        
        queue_id = trigger_build(server, args.job)
        build_num = get_build_number(server, queue_id)
        if not build_num: 
            print("   [FATAL] Could not get build number.")
            return False
        
        result = wait_for_completion(server, args.job, build_num)
        if result != 'SUCCESS':
            print(f"   [FATAL] Build failed with status: {result}")
            return False

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

# ==========================================
# MAIN EXECUTION
# ==========================================
def main():
    args = parse_arguments()
    success = run_test_suite(args)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()