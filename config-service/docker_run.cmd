@echo off
rem Start config service cluster
docker run -d -p 18881:8888 myapp:v1
rem docker run -d -p 18882:8888 myapp:v1
rem docker run -d -p 18883:8888 myapp:v1