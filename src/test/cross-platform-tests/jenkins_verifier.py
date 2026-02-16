"""
Cross-Platform Test Runner
===========================
Usage:
    python jenkins_verifier.py --url <URL> --user <USER> --token <TOKEN> --job <JOB_NAME>
"""

import jenkins
import argparse
import sys
import utils

ARTIFACTS_TO_VERIFY = [
    {"name": "JUnit XML", "filename": "junittestresults.xml", "expected_content": "testAddition"},
    {"name": "TAP Results", "filename": "taptestresults.tap", "expected_content": "testAddition"},
     {"name": "PDF Report", "filename": "testreport.pdf", "expected_content": "%PDF"},
    {"name": "Cobertura XML", "filename": "cobertura.xml", "expected_content": "coverage"}
]

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
        queue_id = utils.trigger_build(server, args.job)
        build_num = utils.get_build_number(server, queue_id)
        
        if not build_num: 
            print("   [FATAL] Could not get build number.")
            return False
        
        result = utils.wait_for_completion(server, args.job, build_num)
        if result != 'SUCCESS':
            print(f"   [FATAL] Build failed with status: {result}")
            return False

        failed = []
        for artifact in ARTIFACTS_TO_VERIFY:
            if not utils.verify_artifact(server, args.url, (args.user, args.token), args.job, build_num, artifact):
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
        print("\n" + "-"*20)
        
        if build_num:
            utils.print_console_output(server, args.job, build_num)
            
            utils.delete_matlab_tools(server, args.job, build_num)
            
            utils.cleanup_build(args.url, (args.user, args.token), args.job, build_num)

# ==========================================
# MAIN EXECUTION
# ==========================================
def main():
    args = parse_arguments()
    success = run_test_suite(args)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()