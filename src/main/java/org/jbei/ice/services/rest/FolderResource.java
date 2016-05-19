package org.jbei.ice.services.rest;

import org.jbei.ice.lib.access.PermissionsController;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dto.folder.FolderDetails;
import org.jbei.ice.lib.dto.folder.FolderType;
import org.jbei.ice.lib.dto.permission.AccessPermission;
import org.jbei.ice.lib.entry.EntrySelection;
import org.jbei.ice.lib.folder.FolderContents;
import org.jbei.ice.lib.folder.FolderController;
import org.jbei.ice.lib.folder.UserFolder;
import org.jbei.ice.lib.shared.ColumnField;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Rest resource for dealing with folders. Note that this is different from collections
 * whose api can be found in the {@link CollectionResource} class.
 * <br>
 * Folders are generally contained in collections and can be created and deleted in an ad-hoc manner
 * while collections are a system defined fixed set.
 *
 * @author Hector Plahar
 */
@Path("/folders")
public class FolderResource extends RestResource {

    private FolderController controller = new FolderController();
    private PermissionsController permissionsController = new PermissionsController();

    /**
     * Creates a new folder with the details specified in the parameter.
     * The default type for the folder is <code>PRIVATE</code> and is owned by the user creating it
     *
     * @param folder details of the folder to create
     * @return information about the created folder including the unique identifier
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(final FolderDetails folder) {
        final String userId = requireUserId();
        log(userId, "creating new folder");
        FolderDetails created = controller.createPersonalFolder(userId, folder);
        return super.respond(created);
    }

    @GET
    @Path("/public")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<FolderDetails> getPublicFolders() {
        return controller.getPublicFolders();
    }

    /**
     * Retrieve specified folder resource
     *
     * @param folderId unique folder resource identifier
     * @return folder specified by resource id, if the user making request has appropriate privileges
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFolder(@PathParam("id") long folderId) {
        String userId = requireUserId();
        log(userId, "get folder \"" + folderId + "\"");
        UserFolder folder = new UserFolder(userId);
        return super.respond(folder.getFolder(folderId));
    }

    /**
     * Updates the specified folder resource
     *
     * @param folderId resource identifier of folder to be updated
     * @param details  details for update
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") final long folderId,
                           final FolderDetails details) {
        final String userId = requireUserId();
        log(userId, "update folder \"" + folderId + "\"");
        final FolderDetails resp = controller.update(userId, folderId, details);
        return super.respond(Response.Status.OK, resp);
    }

    /**
     * Deletes the specified folder resource
     *
     * @return the details of the deleted collection
     */
    @DELETE
    @Path("/{id}")
    public FolderDetails deleteFolder(@PathParam("id") final long folderId,
                                      @QueryParam("type") final String folderType) {
        final String userId = getUserId();
        final FolderType type = FolderType.valueOf(folderType);
        log(userId, "deleting " + type + " folder " + folderId);
        return controller.delete(userId, folderId, type);
    }

    /**
     * Adds contents referenced in the <code>entrySelection</code> object
     * to the folders also referenced in the same object
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/entries")
    public Response addSelectedEntriesToFolders(final EntrySelection entrySelection) {
        final String userId = requireUserId();
        log(userId, "adding entries to folders");
        final FolderContents folderContents = new FolderContents();
        folderContents.addEntrySelection(userId, entrySelection);
        return super.respond(true);
    }

    /**
     * Modifies the contents of a folder either by removing or moving entries as determined by the <code>move</code>
     * parameter
     *
     * @param folderId       resource identifier for folder whose contents are to be modified
     * @param move           whether to move the specified entries or simply remove them from the folder
     * @param entrySelection wrapper around context for modification
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/entries")
    public Response modifyFolderEntries(@PathParam("id") long folderId,
                                        @DefaultValue("false") @QueryParam("move") boolean move,
                                        EntrySelection entrySelection) {
        String userId = requireUserId();
        super.log(userId, "modifying entries for folder " + folderId);
        FolderContents folderContents = new FolderContents();
        boolean success = folderContents.removeFolderContents(userId, folderId, entrySelection, move);
        return super.respond(success);
    }

    /**
     * @return details of the selected collection
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/entries")
    public FolderDetails read(@Context final UriInfo uriInfo,
                              @PathParam("id") final String folderId,
                              @DefaultValue("0") @QueryParam("offset") final int offset,
                              @DefaultValue("15") @QueryParam("limit") final int limit,
                              @DefaultValue("created") @QueryParam("sort") final String sort,
                              @DefaultValue("false") @QueryParam("asc") final boolean asc,
                              @DefaultValue("") @QueryParam("filter") String filter,
                              @QueryParam("fields") List<String> queryParam) {
        final ColumnField field = ColumnField.valueOf(sort.toUpperCase());
        if (folderId.equalsIgnoreCase("public")) {
            // return public entries
            log(uriInfo.getBaseUri().toString(), "requesting public entries");
            return controller.getPublicEntries(field, offset, limit, asc);
        }

        // userId can be empty for public folders
        final String userId = super.getUserId();

        try {
            final long id = Long.decode(folderId);
            String message = "retrieving folder " + id + " entries";
            if (filter.length() > 0)
                message += " filtered by \"" + filter + "\"";
            log(userId, message);
            FolderContents folderContents = new FolderContents();
            return folderContents.getContents(userId, id, field, asc, offset, limit, filter);
        } catch (final NumberFormatException nfe) {
            Logger.error("Passed folder id " + folderId + " is not a number");
            return null;
        }
    }

    /**
     * @return Response with permissions on a collection
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/permissions")
    public Response getFolderPermissions(@PathParam("id") final long folderId) {
        final String userId = getUserId();
        return respond(controller.getPermissions(userId, folderId));
    }

    /**
     * @return details of the modified collection
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/permissions")
    public FolderDetails setPermissions(@Context final UriInfo info,
                                        @PathParam("id") final long folderId,
                                        final ArrayList<AccessPermission> permissions) {
        final String userId = getUserId();
        return permissionsController.setFolderPermissions(userId, folderId, permissions);
    }

    /**
     * @return the added permission
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/permissions")
    public AccessPermission addPermission(@Context final UriInfo info,
                                          @PathParam("id") final long folderId,
                                          final AccessPermission permission) {
        final String userId = getUserId();
        return controller.createFolderPermission(userId, folderId, permission);
    }

    /**
     * @return Response for success or failure
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/permissions/{permissionId}")
    public Response removePermission(@Context final UriInfo info,
                                     @PathParam("id") final long partId,
                                     @PathParam("permissionId") final long permissionId) {
        final String userId = getUserId();
        permissionsController.removeFolderPermission(userId, partId, permissionId);
        return Response.ok().build();
    }

    /**
     * @return Response for success or failure
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/permissions/public")
    public Response enablePublicAccess(@Context final UriInfo info,
                                       @PathParam("id") final long folderId) {
        final String userId = getUserId();
        if (controller.enablePublicReadAccess(userId, folderId)) {
            return respond(Response.Status.OK);
        }
        return respond(Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * @return Response for success or failure
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/permissions/public")
    public Response disablePublicAccess(@Context final UriInfo info,
                                        @PathParam("id") final long folderId) {
        final String userId = getUserId();
        if (controller.disablePublicReadAccess(userId, folderId)) {
            return respond(Response.Status.OK);
        }
        return respond(Response.Status.INTERNAL_SERVER_ERROR);
    }
}
