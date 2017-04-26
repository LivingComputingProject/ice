package org.jbei.ice.services.rest;

import org.jbei.ice.storage.hibernate.HibernateUtil;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Request filter which begins transaction for request. This is the main filter
 * for all requests from the servlet container. {@see IceResponseFilter} for response filter which also
 * contains transaction close/commit
 *
 * @author Hector Plahar
 */
@Provider
public class IceRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // todo :
//        ContainerRequest request = (ContainerRequest) requestContext;
//        String path = request.getPath(true);
//        String method = request.getMethod();
//
////        if (needsSessionId(path, method)) {
//        String auth = requestContext.getHeaderString(Headers.AUTHENTICATION_PARAM_NAME);
//            if (auth == null) {
//                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
//                                                 .entity("User cannot access the resource.")
//                                                 .build());
//            }
//        }

        HibernateUtil.beginTransaction();
    }
}
