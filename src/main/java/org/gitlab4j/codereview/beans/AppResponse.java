package org.gitlab4j.codereview.beans;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AppResponse<T> {

    public static enum Status {
        OK, FAILED, NO_ACTION;
    }

    private boolean success;
    private Status status;
    private String statusText;
    private T data;

    public AppResponse(Status status, T data) {
        this.data = data;
        this.status = status;
        this.success = !Status.FAILED.equals(status);
    }

    public AppResponse(Status status, String statusText, T data) {
        this.data = data;
        this.statusText = statusText;
        this.status = status;
        this.success = !Status.FAILED.equals(status);
    }

    /**
     * 
     * @param success
     * @param message
     * @return
     */
    public static final Response getMessageResponse(boolean success, String message) {
        Status status = success ? Status.OK : Status.FAILED;
        AppResponse<String> response = new AppResponse<String>(status, message, message);
        return (Response.ok().entity(response).type(MediaType.APPLICATION_JSON).build());
    }

    public static final Response getMessageResponse(Status status, String message) {
        AppResponse<String> response = new AppResponse<String>(status, message, message);
        return (Response.ok().entity(response).type(MediaType.APPLICATION_JSON).build());
    }

    public static final <T> Response getDataResponse(boolean success, T data) {
        Status status = success ? Status.OK : Status.FAILED;
        AppResponse<T> response = new AppResponse<T>(status, data);
        return (Response.ok().entity(response).type(MediaType.APPLICATION_JSON).build());
    }

    public static final <T> Response getResponse(Status status, String message, T data) {
        AppResponse<T> response = new AppResponse<T>(status, message, data);
        return (Response.ok().entity(response).type(MediaType.APPLICATION_JSON).build());
    }

    /**
     * @return the success
     */
    public boolean getSuccess() {
        return success;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Status status) {

        this.status = status;
        this.success = Status.OK.equals(status);
    }

    /**
     * @return the statusText
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * @param statusText the statusText to set
     */
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }
}
