# FTPImplementation 

## An implementation of FTP protocol, including client & server.

## Introduction

### Steps to establish the connection:
1. client send *USER* to server
2. client send *PASS* to server 
3. logged in if username & password are **valid**. 

### Control socket & Data socket

1. control socket: used for commands transferring

2. data socket: used for data transferring

### Data Transfer Mode: PASV  
1. client send *PASV* to client
2. server will return its **ip address** & **port**
3. socket for data transferring establishs with specified ip & port 
4. data socket will be reestablished for each data transferring



