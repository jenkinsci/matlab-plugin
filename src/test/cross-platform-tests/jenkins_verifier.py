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
import json
import os
import xml.etree.ElementTree as ET

def load_config(config_path):
    if not os.path.exists(config_path):
        print(f"[ERROR] Configuration file '{config_path}' not found.")
        sys.exit(1)
    
    with open(config_path, 'r') as f:
        return json.load(f)

def configure_freestyle_job(server, job_name, config_map):
    print(f"   [CONFIG] Fetching and updating config.xml for '{job_name}'...")
    config_xml = server.get_job_config(job_name)
    ET.register_namespace("", "")
    root = ET.fromstring(config_xml)
    
    base_class_package = "com.mathworks.ci.freestyle.RunMatlabTestsBuilder$"
    changes = 0

    for tag_name, (class_suffix, file_path) in config_map.items():
        for elem in root.iter(tag_name):
            new_class = f"{base_class_package}{class_suffix}"
            current_class = elem.get('class', '')
            
            current_xml_str = ET.tostring(elem, encoding='unicode')
            path_needs_update = file_path and (file_path not in current_xml_str)
            
            if current_class != new_class or path_needs_update:
                elem.set('class', new_class)
                
                for child in list(elem): 
                    elem.remove(child) 
                
                if class_suffix == "NullArtifact":
                    elem.text = "false"
                else:
                    elem.text = None
                    path_elem = ET.Element("filePath")
                    path_elem.text = file_path
                    elem.append(path_elem)
                changes += 1

    for archiver in root.iter("hudson.tasks.ArtifactArchiver"):
        for artifacts_tag in archiver.iter("artifacts"):
            current_pattern = artifacts_tag.text
            new_pattern = "matlabTestArtifacts/**/*" 
            
            if current_pattern != new_pattern:
                artifacts_tag.text = new_pattern
                changes += 1

    if changes > 0:
        new_config_str = ET.tostring(root, encoding='utf-8', method='xml').decode('utf-8')
        server.reconfig_job(job_name, new_config_str)
        print("   [CONFIG] Job reconfigured.")
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

def verify_console_log(server, job_name, build_number, expected_text):
    print(f"   [TEST] Verifying Console Log...")
    try:
        console_output = server.get_build_console_output(job_name, build_number)
        if expected_text in console_output:
            print(f"      -> [PASS] Found expected log: '{expected_text}'")
            return True
        else:
            print(f"      -> [FAIL] Log missing text: '{expected_text}'")
            return False
    except Exception as e:
        print(f"      -> [ERROR] Failed to fetch logs: {e}")
        return False

def verify_artifact(server, base_url, auth, job_name, build_number, case):
    print(f"   [TEST] Verifying Artifact: {case['ARTIFACT_NAME']}")
    try:
        info = server.get_build_info(job_name, build_number)
        artifacts = info.get('artifacts', [])
        found = next((a for a in artifacts if case['ARTIFACT_NAME'] in a['fileName']), None)
        
        if not found:
            print(f"      -> [FAIL] Artifact not found in Jenkins archive.")
            return False
            
        url = f"{base_url.rstrip('/')}/job/{job_name}/{build_number}/artifact/{found['relativePath']}"
        res = requests.get(url, auth=auth)
        
        snippet = res.content[:100].decode('utf-8', errors='ignore')
        if case['EXPECTED_CONTENT'] in snippet or case['EXPECTED_CONTENT'] in res.text:
            print(f"      -> [PASS] Content verified.")
            return True
        else:
            print(f"      -> [FAIL] Content mismatch. Found: {snippet}")
            return False
    except Exception as e:
        print(f"      -> [ERROR] {e}")
        return False

# ==========================================
# MAIN EXECUTION
# ==========================================
def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    default_config_path = os.path.join(script_dir, "config.json")

    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--user", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--job", required=True)
    parser.add_argument("--config", default=default_config_path, help="Path to config file")
    args = parser.parse_args()

    test_suite = load_config(args.config)
    server = jenkins.Jenkins(args.url, username=args.user, password=args.token)
    failed = []

    print("=== STARTING TEST SUITE ===")

    for case in test_suite:
        print(f"\nRUNNING: {case['CASE_NAME']}")
        try:
            # 1. Configure Job
            configure_freestyle_job(server, args.job, case['CONFIG'])
            
            # 2. Run Build
            queue_id = trigger_build(server, args.job)
            build_num = get_build_number(server, queue_id)
            if not build_num: 
                failed.append(case['CASE_NAME'])
                continue
            
            # 3. Wait for Finish
            result = wait_for_completion(server, args.job, build_num)
            if result != 'SUCCESS':
                print(f"      -> Build Failed ({result})")
                failed.append(case['CASE_NAME'])
                continue

            # 4. Verify Log
            log_check = True
            if "EXPECTED_LOG" in case:
                log_check = verify_console_log(server, args.job, build_num, case["EXPECTED_LOG"])
            
            # 5. Verify Artifact
            artifact_check = verify_artifact(server, args.url, (args.user, args.token), args.job, build_num, case)

            if not (log_check and artifact_check):
                failed.append(case['CASE_NAME'])

        except Exception as e:
            print(f"      -> [CRITICAL ERROR] {e}")
            failed.append(case['CASE_NAME'])

    print("\n" + "="*40)
    if failed:
        print(f"FAILURES: {failed}")
        sys.exit(1)
    else:
        print("SUCCESS: All scenarios passed.")
        sys.exit(0)

if __name__ == "__main__":
    main()