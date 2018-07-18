package org.gitlab4j.codereview.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.gitlab4j.api.GitLabApiException;

public class ResponseException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    public ResponseException(int httpStatus) {
        super(Response.status(httpStatus).build());
    }

    public ResponseException(Status status) {
        super(Response.status(status).build());
    }

    public ResponseException(GitLabApiException glae) {
        super(Response.status(glae.getHttpStatus()).entity(glae.getMessage()).type(MediaType.TEXT_PLAIN).build());
    }

    public ResponseException(int httpStatus, String message) {
        super(Response.status(httpStatus).entity(message).type(MediaType.TEXT_PLAIN).build());
    }

    public ResponseException(Status status, String message) {
        super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build());
    }
}
