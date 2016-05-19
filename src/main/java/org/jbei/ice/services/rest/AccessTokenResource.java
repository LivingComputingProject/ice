package org.jbei.ice.services.rest;

import org.jbei.ice.lib.account.AccountController;
import org.jbei.ice.lib.account.AccountTransfer;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dto.web.RegistryPartner;
import org.jbei.ice.lib.net.WebPartners;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * API for access tokens (also session id for the user interface)
 *
 * @author Hector Plahar
 */
@Path("/accesstokens")
public class AccessTokenResource extends RestResource {

    private final AccountController accountController = new AccountController();

    /**
     * Creates a new access token for the user referenced in the parameter, after the credentials
     * (username and password) are validated. If one already exists, it is invalidated
     *
     * @param transfer wraps username and password
     * @return account information including a valid session id if credentials validate
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(final AccountTransfer transfer) {
        final AccountTransfer info = accountController.authenticate(transfer);
        if (info == null) {
            Logger.warn("Authentication failed for user " + transfer.getEmail());
            return respond(Response.Status.UNAUTHORIZED);
        }

        Logger.info("User '" + transfer.getEmail() + "' successfully logged in");
        return respond(Response.Status.OK, info);
    }

    /**
     * Invalidates the current session information.
     */
    @DELETE
    public Response deleteToken() {
        // ensure the user is valid
        String userId = requireUserId();
        log(userId, "logging out");
        accountController.invalidate(userId);
        return super.respond(Response.Status.OK);
    }

    /**
     * Retrieve account information for user referenced by session id
     *
     * @param sessionId unique session identifier for logged in user
     * @return account information for session if session is valid, null otherwise
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@HeaderParam(AUTHENTICATION_PARAM_NAME) String sessionId) {
        String userId = requireUserId();
        AccountTransfer transfer = accountController.getByEmail(userId).toDataTransferObject();
        transfer.setAdmin(accountController.isAdministrator(userId));
        return super.respond(transfer);
    }

    /**
     * Validates web of registries access token (api key)
     */
    @GET
    @Path("/web")
    public Response getWebPartner(@HeaderParam(WOR_PARTNER_TOKEN) String token,
                                  @QueryParam("url") String url) {
        WebPartners partners = new WebPartners();
        RegistryPartner partner = partners.get(token, url);
        return super.respond(partner);
    }
}
