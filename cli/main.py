import requests
import time
import sys
import argparse

def fail(message):
    print("ERROR:", message)
    sys.exit(1)

def submit_task(manager_url, command, cpu, memory):
    print(f"Submitting task: '{command}' with CPU={cpu}, Memory={memory}MB")
    response = requests.post(
        f"{manager_url}/task/submit",
        json={
            "command": command,
            "resources": {"cpuCount": cpu, "memoryMb": memory}
        },
        headers={"Accept": "application/json"}
    )

    if response.status_code != 200:
        fail(f"Failed to submit task: {response.status_code} {response.text}")

    data = response.json()
    execution_id = data.get("executionId")
    if not execution_id:
        fail("No executionId returned from manager")

    print(f"Task submitted with executionId: {execution_id}")
    return execution_id

def wait_for_completion(manager_url, execution_id, timeout=30):
    print("Polling for task completion...")
    start = time.time()
    while time.time() - start < timeout:
        response = requests.get(f"{manager_url}/task/{execution_id}")
        if response.status_code != 200:
            fail(f"Failed to fetch task status: {response.status_code} {response.text}")

        execution = response.json()
        status = execution.get("status")
        print(f"Status: {status}")

        if status == "FINISHED":
            print("Task finished")
            print("Result:", execution.get("result"))
            return
        elif status == "FAILED":
            fail(f"Task failed: {execution.get('error')}")

        time.sleep(1)

    fail("Task timed out")

def main():
    parser = argparse.ArgumentParser(description="CLI for submitting tasks to manager")
    parser.add_argument("manager_url", help="URL of the manager service, e.g. http://localhost:8080")
    parser.add_argument("command", help="Command to execute, e.g. 'echo hello'")
    parser.add_argument("--cpu", type=int, default=1, help="CPU cores required (default 1)")
    parser.add_argument("--memory", type=int, default=128, help="Memory required in MB (default 128)")
    parser.add_argument("--timeout", type=int, default=30, help="Timeout in seconds to wait for completion (default 30)")

    args = parser.parse_args()
    manager_url = args.manager_url.rstrip("/")

    execution_id = submit_task(manager_url, args.command, args.cpu, args.memory)
    wait_for_completion(manager_url, execution_id, timeout=args.timeout)

if __name__ == "__main__":
    main()