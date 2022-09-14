package org.opennms.plugins.cloud.srv.appliance;

public class OnmsHttpInfo {
    private String httpUrl;
    private String httpUser;
    private String httpPassword;

    public OnmsHttpInfo() {
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getHttpUser() {
        return httpUser;
    }

    public void setHttpUser(String httpUser) {
        this.httpUser = httpUser;
    }

    public String getHttpPassword() {
        return httpPassword;
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword;
    }

    @Override
    public String toString() {
        return "OnmsHttpInfo{" +
                "httpUrl='" + httpUrl + '\'' +
                ", httpUser='" + httpUser + '\'' +
                ", httpPassword='" + httpPassword + '\'' +
                '}';
    }
}
