// IIPCExample.aidl
package com.example.messengerserverapplication;

// Declare any non-default types here with import statements

interface IIPCExample {
    /** Request the process ID of this service */
    int getPid();

    /** Count of received connection requests from clients */
    int getConnectionCount();

    /** Set displayed value of screen */
    void postVal(String packageName, int pid, String data);
}