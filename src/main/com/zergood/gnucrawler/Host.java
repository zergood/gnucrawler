package com.zergood.gnucrawler;

public class Host {

    private String ipAddress;
    private int port;

    private boolean isUltrapeer;
    private int sharedFileCount;
    private int sharedFileSize;

    private String response = "";

    private boolean readFlag = false;
    private boolean writeFlag = false;

    /**
     * Constructs a Host
     *
     * @param ipAddress       IP address
     * @param port            port
     * @param sharedFileCount count of shared files
     * @param sharedFileSize  total shared file size in KB
     */
    public Host(String ipAddress, int port, int sharedFileCount, int sharedFileSize) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.isUltrapeer = false;
        this.sharedFileCount = sharedFileCount;
        this.sharedFileSize = sharedFileSize;
    }

    public Host(String ipAddress, int port, boolean isUltrapeer){
        this.ipAddress = ipAddress;
        this.port = port;
        this.isUltrapeer = isUltrapeer;
    }

    /**
     * Get text based host information
     */
    @Override
    public String toString() {
        return ipAddress + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        if (port != host.port) return false;
        if (ipAddress != null ? !ipAddress.equals(host.ipAddress) : host.ipAddress != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = ipAddress != null ? ipAddress.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (isUltrapeer ? 1 : 0);
        return result;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    /**
     * Sets whether this host is an Ultrapeer
     */
    public void setUltrapeer(boolean ultrapeer) {
        this.isUltrapeer = ultrapeer;
    }

    /**
     * Return true if this host is an Ultrapeer, false otherwise
     */
    public boolean getUltrapeer() {
        return isUltrapeer;
    }

    /**
     * Return shared file count
     */
    public int getSharedFileCount() {
        return sharedFileCount;
    }

    /**
     * Reurn the shared file size
     */
    public int getSharedFileSize() {
        return sharedFileSize;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isReadFlag() {
        return readFlag;
    }

    public void setReadFlag(boolean readFlag) {
        this.readFlag = readFlag;
    }

    public boolean isWriteFlag() {
        return writeFlag;
    }

    public void setWriteFlag(boolean writeFlag) {
        this.writeFlag = writeFlag;
    }
}
