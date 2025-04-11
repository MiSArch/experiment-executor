import time
import docker
from typing import List

def are_containers_running(names: List[str]) -> bool:
    client = docker.from_env()
    try:
        for name in names:
            container = client.containers.get(name)
            if container.status != "running":
                return False
        return True
    except docker.errors.NotFound:
        return False

def kill_containers(names: List[str]):
    client = docker.from_env()
    for name in names:
        try:
            container = client.containers.get(name)
            container.kill()
        except docker.errors.NotFound:
            print(f"Container {name} not found.")

def start_containers(names: List[str]):
    client = docker.from_env()
    for name in names:
        try:
            container = client.containers.get(name)
            container.start()
        except docker.errors.NotFound:
            print(f"Container {name} not found.")

def sleep(seconds: float):
    time.sleep(seconds)