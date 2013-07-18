package org.jbei.ice.services.webservices;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.jbei.ice.controllers.ControllerFactory;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.account.AccountController;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.authentication.InvalidCredentialsException;
import org.jbei.ice.lib.composers.formatters.FastaFormatter;
import org.jbei.ice.lib.composers.formatters.GenbankFormatter;
import org.jbei.ice.lib.config.ConfigurationController;
import org.jbei.ice.lib.entry.EntryController;
import org.jbei.ice.lib.entry.model.ArabidopsisSeed;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.entry.model.EntryFundingSource;
import org.jbei.ice.lib.entry.model.Link;
import org.jbei.ice.lib.entry.model.Part;
import org.jbei.ice.lib.entry.model.Plasmid;
import org.jbei.ice.lib.entry.model.Strain;
import org.jbei.ice.lib.entry.sample.SampleController;
import org.jbei.ice.lib.entry.sample.StorageController;
import org.jbei.ice.lib.entry.sample.model.Sample;
import org.jbei.ice.lib.entry.sequence.SequenceAnalysisController;
import org.jbei.ice.lib.entry.sequence.SequenceController;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.models.SelectionMarker;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.Storage;
import org.jbei.ice.lib.models.Storage.StorageType;
import org.jbei.ice.lib.models.TraceSequence;
import org.jbei.ice.lib.net.WoRController;
import org.jbei.ice.lib.parsers.GeneralParser;
import org.jbei.ice.lib.permissions.PermissionException;
import org.jbei.ice.lib.search.SearchController;
import org.jbei.ice.lib.shared.dto.ConfigurationKey;
import org.jbei.ice.lib.shared.dto.entry.EntryType;
import org.jbei.ice.lib.shared.dto.entry.PartData;
import org.jbei.ice.lib.shared.dto.search.SearchQuery;
import org.jbei.ice.lib.shared.dto.search.SearchResults;
import org.jbei.ice.lib.shared.dto.user.User;
import org.jbei.ice.lib.utils.SerializationUtils;
import org.jbei.ice.lib.vo.FeaturedDNASequence;
import org.jbei.ice.lib.vo.IDNASequence;
import org.jbei.ice.lib.vo.SequenceTraceFile;
import org.jbei.ice.server.ModelToInfoFactory;

/**
 * SOAP API methods.
 *
 * @author Hector Plahar, Zinovii Dmytriv, Timothy Ham
 */
@WebService(targetNamespace = "https://api.registry.jbei.org/")
public class RegistryAPI implements IRegistryAPI {
    /**
     * Login to the ICE SOAP service, with the given login and password. Returns a session key for
     * future authentication.
     *
     * @param login    Login.
     * @param password Password.
     * @return Session key.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public String login(@WebParam(name = "login") String login,
            @WebParam(name = "password") String password) throws SessionException, ServiceException {
        String sessionId;

        try {
            AccountController controller = ControllerFactory.getAccountController();
            User info = controller.authenticate(login, password);
            sessionId = info.getSessionId();
        } catch (InvalidCredentialsException e) {
            Logger.warn("Invalid credentials provided by user: " + login);
            throw new SessionException("Invalid credentials!");
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        log("User by login '" + login + "' successfully logged in");
        return sessionId;
    }

    /**
     * Logout out of the ICE SOAP service by deauthenticating the session
     *
     * @param sessionId Session key to log out.
     * @throws ServiceException
     */
    @Override
    public void logout(@WebParam(name = "sessionId") String sessionId) throws ServiceException {
        try {
            AccountController.deauthenticate(sessionId);
            log(sessionId, "Logged out");
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    /**
     * Check if the session key is still authenticated.
     *
     * @param sessionId Session key to check.
     * @return True if still authenticated, false otherwise
     * @throws ServiceException
     */
    @Override
    public boolean isAuthenticated(@WebParam(name = "sessionId") String sessionId) throws ServiceException {
        boolean authenticated;

        try {
            authenticated = AccountController.isAuthenticated(sessionId);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return authenticated;
    }

    /**
     * Check if the Account associated with the session key has administrative privileges
     *
     * @param sessionId valid session id to ensure user successfully logged in.
     * @param login     user login being checked for moderator status
     * @return true if user has administrative privileges, false otherwise
     */
    @Override
    public boolean isAdministrator(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "login") String login) throws SessionException, ServiceException {
        Account account = validateAccount(sessionId);
        AccountController controller = ControllerFactory.getAccountController();

        try {
            return controller.isAdministrator(account);
        } catch (ControllerException e) {
            Logger.error(e);
            throw new ServiceException("Error accessing account for " + login);
        }
    }

    /**
     * Retrieve an {@link Entry} by its name.
     *
     * @param sessionId Session key.
     * @param name      Name of the Entry to retrieve.
     * @return Entry object.
     * @throws ServiceException
     */
    @Override
    public PartData getEntryByName(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "name") String name) throws ServiceException {
        log(sessionId, "getEntryByName: " + name);
        try {
            Account account = validateAccount(sessionId);
            return ControllerFactory.getEntryController().getByUniqueName(account, name);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    @Override
    public boolean hasSequence(@WebParam(name = "recordId") String recordId) throws ServiceException {
        try {
            log("hasSequence " + recordId);
            PartData entry = ControllerFactory.getEntryController().getPublicEntryByRecordId(recordId);
            return ControllerFactory.getSequenceController().hasSequence(entry.getId());
        } catch (ControllerException ce) {
            throw new ServiceException(ce);
        }
    }

    @Override
    public boolean hasOriginalSequence(@WebParam(name = "recordId") String recordId) throws ServiceException {
        try {
            log("hasSequence " + recordId);
            PartData entry = ControllerFactory.getEntryController().getPublicEntryByRecordId(recordId);
            return ControllerFactory.getSequenceController().hasOriginalSequence(entry.getId());
        } catch (ControllerException ce) {
            throw new ServiceException(ce);
        }
    }

    /**
     * Retrieve {@link Entry} by its recordId.
     *
     * @param sessionId Session key.
     * @param entryId   recordId of the Entry.
     * @return Entry object.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public PartData getByRecordId(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "getByRecordId: " + entryId);
        Account account = validateAccount(sessionId);

        try {
            Entry entry = ControllerFactory.getEntryController().getByRecordId(account, entryId);
            SequenceController sequenceController = ControllerFactory.getSequenceController();
            PartData info = ModelToInfoFactory.getInfo(null, entry, null, null, null);
            boolean hasSequence = sequenceController.hasSequence(entry.getId());
            info.setHasSequence(hasSequence);
            boolean hasOriginalSequence = sequenceController.hasOriginalSequence(entry.getId());
            info.setHasOriginalSequence(hasOriginalSequence);
            return info;
        } catch (PermissionException e) {
            log(account.getEmail() + ": attempting to retrieve entry " + entryId + " without access permissions");
            return null;
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    @Override
    public PartData getPublicEntryByRecordId(@WebParam(name = "recordId") String recordId) throws ServiceException {
        log("getByRecordId " + recordId);
        try {
            return ControllerFactory.getEntryController().getPublicEntryByRecordId(recordId);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    @Override
    public PartData getPublicEntryById(@WebParam(name = "entryId") long entryId) throws ServiceException {
        log("getByEntryId" + entryId);
        try {
            return ControllerFactory.getEntryController().getPublicEntryById(entryId);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    /**
     * Retrieve an {@link Entry} by its part number.
     *
     * @param sessionId  Session key.
     * @param partNumber Part number of the desired Entry.
     * @return Entry object.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public PartData getByPartNumber(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "partNumber") String partNumber) throws SessionException, ServiceException {
        log(sessionId, "getByPartNumber: " + partNumber);
        Account account = validateAccount(sessionId);

        try {
            EntryController entryController = ControllerFactory.getEntryController();
            return entryController.getByPartNumber(account, partNumber);
        } catch (PermissionException e) {
            Logger.error(account.getEmail() + " attempting to retrieve entry by part number " + partNumber
                                 + " without access permissions");
            return null;
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    /**
     * Check if the user referenced by the session id has write permission to the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   recordId of the Entry.
     * @return True if the session user has write permission.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public boolean hasWritePermissions(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") long entryId) throws SessionException, ServiceException {
        log(sessionId, "hasWritePermissions: " + entryId);
        Account account = validateAccount(sessionId);

        try {
            return ControllerFactory.getEntryController().hasWritePermission(account, entryId);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    /**
     * Create a new {@link Plasmid} on the server.
     *
     * @param sessionId Session key.
     * @param plasmid   Plasmid to create.
     * @return New Plasmid object from the database.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public Plasmid createPlasmid(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "plasmid") Plasmid plasmid) throws SessionException, ServiceException {
        log(sessionId, "createPlasmid");
        Entry newEntry;
        Account account = validateAccount(sessionId);
        try {
            EntryController entryController = ControllerFactory.getEntryController();
            Entry remoteEntry = createEntry(plasmid);
            newEntry = entryController.createEntry(account, remoteEntry, null);
            log("User '" + account.getEmail() + "' created plasmid: '" + plasmid.getRecordId()
                        + "', " + plasmid.getId());
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Plasmid) newEntry;
    }

    /**
     * Create a new {@link Strain} on the server.
     *
     * @param sessionId Session key.
     * @param strain    Strain to create.
     * @return New Strain object from the database.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public Strain createStrain(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "strain") Strain strain) throws SessionException, ServiceException {
        log(sessionId, "createStrain");
        Entry newEntry;
        Account account = validateAccount(sessionId);

        try {
            EntryController entryController = ControllerFactory.getEntryController();
            Entry remoteEntry = createEntry(strain);
            newEntry = entryController.createEntry(account, remoteEntry, null);
            log("User '" + account.getEmail() + "' created strain: '" + strain.getRecordId() + "', " + strain.getId());
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Strain) newEntry;
    }

    /**
     * Create a new {@link Part} on the server.
     *
     * @param sessionId Session key.
     * @param part      Part to create.
     * @return New Part object from the database.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public Part createPart(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "part") Part part) throws SessionException, ServiceException {
        log(sessionId, "createPart");
        Entry newEntry;
        Account account = validateAccount(sessionId);
        try {
            EntryController entryController = ControllerFactory.getEntryController();
            Entry remoteEntry = createEntry(part);
            newEntry = entryController.createEntry(account, remoteEntry, null);
            log("User '" + account.getEmail() + "' created part: '" + part.getRecordId() + "', " + part.getId());
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Part) newEntry;
    }

    @Override
    public ArabidopsisSeed createSeed(@WebParam(name = "sessionId") String sessionId, @WebParam(
            name = "seed") ArabidopsisSeed seed) throws SessionException, ServiceException {
        log(sessionId, "createSeed");
        Entry newEntry;
        Account account = validateAccount(sessionId);
        try {
            EntryController controller = ControllerFactory.getEntryController();
            Entry remoteEntry = createEntry(seed);
            newEntry = controller.createEntry(account, remoteEntry, null);
            log("User '" + account.getEmail() + "' created arabidopsis seed: '" + seed.getRecordId());
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return (ArabidopsisSeed) newEntry;
    }

//    /**
//     * Save the given {@link Plasmid} on the server.
//     *
//     * @param sessionId Session key.
//     * @param plasmid   Plasmid to save.
//     * @return Saved Plasmid object.
//     * @throws SessionException
//     * @throws ServiceException
//     */
//    @Override
//    public Plasmid updatePlasmid(@WebParam(name = "sessionId") String sessionId,
//            @WebParam(name = "plasmid") Plasmid plasmid) throws SessionException, ServiceException {
//        log(sessionId, "updatePlasmid");
//        Entry savedEntry;
//        Account account = validateAccount(sessionId);
//
//        try {
//            EntryController entryController = ControllerFactory.getEntryController();
//            savedEntry = entryController.update(account, updateEntry(account, plasmid));
//            log("User '" + account.getEmail() + "' update plasmid: '" + savedEntry.getRecordId()
//                        + "', " + savedEntry.getId());
//        } catch (Exception e) {
//            Logger.error(e);
//            throw new ServiceException("Registry Service Internal Error!");
//        }
//
//        return (Plasmid) savedEntry;
//    }

    /**
     * Save the given {@link Strain} on the server.
     *
     * @param sessionId Session key.
     * @param strain    Strain to save.
     * @return Saved Strain object.
     * @throws SessionException
     * @throws ServiceException
     */
//    @Override
//    public Strain updateStrain(@WebParam(name = "sessionId") String sessionId,
//            @WebParam(name = "strain") Strain strain) throws SessionException, ServiceException {
//        log(sessionId, "updateStrain");
//        Entry savedEntry;
//        Account account = validateAccount(sessionId);
//
//        try {
//            EntryController entryController = ControllerFactory.getEntryController();
//            savedEntry = entryController.update(account, updateEntry(account, strain));
//            log("User '" + account.getEmail() + "' update strain: '" + savedEntry.getRecordId()
//                        + "', " + savedEntry.getId());
//        } catch (Exception e) {
//            Logger.error(e);
//            throw new ServiceException("Registry Service Internal Error!");
//        }
//
//        return (Strain) savedEntry;
//    }

    /**
     * Save the given {@link Part} on the server.
     *
     * @param sessionId Session key.
     * @param part      Part to save.
     * @return Saved Part object.
     * @throws SessionException
     * @throws ServiceException
     * @throws ServicePermissionException
     */
//    @Override
//    public Part updatePart(@WebParam(name = "sessionId") String sessionId,
//            @WebParam(name = "part") Part part) throws SessionException, ServiceException,
//            ServicePermissionException {
//        log(sessionId, "updatePart");
//        Entry savedEntry;
//        Account account = validateAccount(sessionId);
//
//        try {
//            EntryController entryController = ControllerFactory.getEntryController();
//            savedEntry = entryController.update(account, updateEntry(account, part));
//            log("User '" + account.getEmail() + "' update part: '" + savedEntry.getRecordId()
//                        + "', " + savedEntry.getId());
//        } catch (Exception e) {
//            Logger.error(e);
//            throw new ServiceException("Registry Service Internal Error!");
//        }
//
//        return (Part) savedEntry;
//    }

    /**
     * Create a generic {@link Entry} on the server.
     * <p/>
     * Perform validation for a generic Entry.
     *
     * @param entry Entry to save.
     * @return Saved Entry object.
     * @throws SessionException
     * @throws ServiceException
     */
    protected Entry createEntry(Entry entry) throws SessionException,
            ServiceException {
        if (entry == null) {
            throw new ServiceException("Failed to create null Entry!");
        }

        // Validate recordType
        if (entry instanceof Plasmid) {
            entry.setRecordType("plasmid");
        } else if (entry instanceof Strain) {
            entry.setRecordType("strain");
        } else if (entry instanceof Part) {
            entry.setRecordType("part");
        } else {
            throw new ServiceException(
                    "Invalid entry class! Accepted entries with classes Plasmid, Strain and Part.");
        }

        // Validate creator
        if (entry.getCreator() == null || entry.getCreator().isEmpty()) {
            throw new ServiceException("Creator is mandatory field!");
        }

        // Validate owner and ownerEmail
        if (entry.getOwner() == null || entry.getOwner().isEmpty() || entry.getOwnerEmail() == null
                || entry.getOwnerEmail().isEmpty()) {
            throw new ServiceException("Owner and OwnerEmail are mandatory fields!");
        }

        // Validate short description
        if (entry.getShortDescription() == null || entry.getShortDescription().isEmpty()) {
            throw new ServiceException("Short Description is mandatory field!");
        }

        // Validate status
        if (entry.getStatus() == null) {
            throw new ServiceException(
                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
        } else if (!entry.getStatus().equals("complete")
                && !entry.getStatus().equals("in progress") && !entry.getStatus().equals("planned")) {
            throw new ServiceException(
                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
        }

        // Validate bioSafetyLevel
        if (entry.getBioSafetyLevel() != 1 && entry.getBioSafetyLevel() != 2) {
            throw new ServiceException("Invalid bio safety level! Expected: '1' or '2'");
        }

        // Validate name
        if (entry.getName() == null || entry.getName().trim().isEmpty()) {
            throw new ServiceException("Name is mandatory!");
        }

        // Validate selection markers
        if (entry.getSelectionMarkers() != null && entry.getSelectionMarkers().size() > 0) {
            for (SelectionMarker selectionMarker : entry.getSelectionMarkers()) {
                if (selectionMarker.getName() == null || selectionMarker.getName().isEmpty()) {
                    throw new ServiceException("Selection Marker can't be null or empty!");
                }

                selectionMarker.setEntry(entry);
            }
        }

        // Validate links
        if (entry.getLinks() != null && entry.getLinks().size() > 0) {
            for (Link link : entry.getLinks()) {
                if (link.getLink() == null || link.getLink().isEmpty()) {
                    throw new ServiceException("Link can't be null or empty!");
                }

                link.setEntry(entry);
            }
        }

        // Validate entry funding sources
        if (entry.getEntryFundingSources() == null || entry.getEntryFundingSources().size() == 0) {
            throw new ServiceException(
                    "FundingSource is mandatory! Expected at least one FundingSource.");
        } else {
            for (EntryFundingSource entryFundingSource : entry.getEntryFundingSources()) {
                if (entryFundingSource.getFundingSource() == null) {
                    throw new ServiceException("FundingSource can't be null!");
                }

                if (entryFundingSource.getFundingSource().getFundingSource() == null
                        || entryFundingSource.getFundingSource().getFundingSource().isEmpty()) {
                    throw new ServiceException("FundingSource can't be null or empty!");
                }

                if (entryFundingSource.getFundingSource().getPrincipalInvestigator() == null
                        || entryFundingSource.getFundingSource().getPrincipalInvestigator().isEmpty()) {
                    throw new ServiceException("PrincipalInvestigator can't be null or empty!");
                }

                entryFundingSource.setEntry(entry);
            }
        }

        return entry;
    }

//    private Entry updateEntry(Account account, Entry entry) throws SessionException, ServiceException {
//        Entry currentEntry;
//        try {
//            EntryController entryController = ControllerFactory.getEntryController();
//            currentEntry = entryController.getByRecordId(account, entry.getRecordId());
//
//            if (currentEntry == null) {
//                throw new ServiceException("Invalid recordId for entry!");
//            }
//
//            if (!permissionsController.hasWritePermission(account, currentEntry)) {
//                throw new ServicePermissionException("No permissions to change this entry!");
//            }
//        } catch (ControllerException | PermissionException e) {
//            throw new ServiceException(e);
//        }
//
//        // Validate and set creator
//        if (entry.getCreator() == null || entry.getCreator().isEmpty()) {
//            throw new ServiceException("Creator is mandatory field!");
//        } else {
//            currentEntry.setCreator(entry.getCreator());
//            currentEntry.setCreatorEmail(entry.getCreatorEmail());
//        }
//
//        // Validate and set owner
//        if (entry.getOwner() == null || entry.getOwner().isEmpty()) {
//            throw new ServiceException("Owner is mandatory field!");
//        } else {
//            currentEntry.setOwner(entry.getOwner());
//        }
//
//        // Validate and set ownerEmail
//        if (entry.getOwnerEmail() == null || entry.getOwnerEmail().isEmpty()) {
//            throw new ServiceException("OwnerEmail is mandatory field!");
//        } else {
//            currentEntry.setOwnerEmail(entry.getOwnerEmail());
//        }
//
//        // Validate and set short description
//        if (entry.getShortDescription() == null || entry.getShortDescription().isEmpty()) {
//            throw new ServiceException("Short Description is mandatory field!");
//        } else {
//            currentEntry.setShortDescription(entry.getShortDescription());
//        }
//
//        // Validate status
//        if (entry.getStatus() == null) {
//            throw new ServiceException(
//                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
//        } else if (!entry.getStatus().equals("complete")
//                && !entry.getStatus().equals("in progress") && !entry.getStatus().equals("planned")) {
//            throw new ServiceException(
//                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
//        } else {
//            currentEntry.setStatus(entry.getStatus());
//        }
//
//        // Validate bioSafetyLevel
//        if (entry.getBioSafetyLevel() != 1 && entry.getBioSafetyLevel() != 2) {
//            throw new ServiceException("Invalid bio safety level! Expected: '1' or '2'");
//        } else {
//            currentEntry.setBioSafetyLevel(entry.getBioSafetyLevel());
//        }
//
//        currentEntry.setAlias(entry.getAlias());
//        currentEntry.setKeywords(entry.getKeywords());
//        currentEntry.setLongDescription(entry.getLongDescription());
//        currentEntry.setReferences(entry.getReferences());
//        currentEntry.setIntellectualProperty(entry.getIntellectualProperty());
//
//        if (entry instanceof Plasmid) {
//            ((Plasmid) currentEntry).setBackbone(((Plasmid) entry).getBackbone());
//            ((Plasmid) currentEntry).setCircular(((Plasmid) entry).getCircular());
//            ((Plasmid) currentEntry).setOriginOfReplication(((Plasmid) entry).getOriginOfReplication());
//            ((Plasmid) currentEntry).setPromoters(((Plasmid) entry).getPromoters());
//        } else if (entry instanceof Strain) {
//            ((Strain) currentEntry).setHost(((Strain) entry).getHost());
//            ((Strain) currentEntry).setPlasmids(((Strain) entry).getPlasmids());
//            ((Strain) currentEntry).setGenotypePhenotype(((Strain) entry).getGenotypePhenotype());
//        }
//
//        // Validate and set name
//        if (entry.getNames() == null || entry.getNames().size() == 0) {
//            throw new ServiceException("Name is mandatory! Expected at least one name.");
//        } else {
//            for (Name name : entry.getNames()) {
//                if (name.getName() == null || name.getName().isEmpty()) {
//                    throw new ServiceException("Name can't be null or empty!");
//                }
//
//                boolean existName = false;
//                for (Name currentEntryName : currentEntry.getNames()) {
//                    if (currentEntryName.getName().equals(name.getName())) {
//                        existName = true;
//
//                        break;
//                    }
//                }
//
//                if (!existName) {
//                    name.setEntry(currentEntry);
//                    currentEntry.getNames().add(name);
//                }
//            }
//        }
//
//        // Validate and set selection markers
//        if (entry.getSelectionMarkers() != null && entry.getSelectionMarkers().size() > 0) {
//            for (SelectionMarker selectionMarker : entry.getSelectionMarkers()) {
//                if (selectionMarker.getName() == null || selectionMarker.getName().isEmpty()) {
//                    throw new ServiceException("Selection Marker can't be null or empty!");
//                }
//
//                boolean existSelectionMarker = false;
//                for (SelectionMarker currentEntrySelectionMarker : currentEntry.getSelectionMarkers()) {
//                    if (currentEntrySelectionMarker.getName().equals(selectionMarker.getName())) {
//                        existSelectionMarker = true;
//                        break;
//                    }
//                }
//
//                if (!existSelectionMarker) {
//                    selectionMarker.setEntry(currentEntry);
//                    currentEntry.getSelectionMarkers().add(selectionMarker);
//                }
//            }
//        } else {
//            currentEntry.setSelectionMarkers(null);
//        }
//
//        if (entry.getLinks() != null && entry.getLinks().size() > 0) {
//            for (Link link : entry.getLinks()) {
//                if (link.getLink() == null || link.getLink().isEmpty()) {
//                    throw new ServiceException("Link can't be null or empty!");
//                }
//
//                boolean existLink = false;
//                for (Link currentEntryLink : currentEntry.getLinks()) {
//                    if (currentEntryLink.getUrl().equals(link.getUrl())
//                            && currentEntryLink.getLink().equals(link.getLink())) {
//                        existLink = true;
//                        break;
//                    }
//                }
//
//                if (!existLink) {
//                    link.setEntry(currentEntry);
//                    currentEntry.getLinks().add(link);
//                }
//            }
//        } else {
//            currentEntry.setLinks(null);
//        }
//
//        // Validate and set entry funding sources
//        if (entry.getEntryFundingSources() == null || entry.getEntryFundingSources().size() == 0) {
//            throw new ServiceException(
//                    "FundingSource is mandatory! Expected at least one FundingSource.");
//        } else {
//            for (EntryFundingSource entryFundingSource : entry.getEntryFundingSources()) {
//                if (entryFundingSource.getFundingSource() == null) {
//                    throw new ServiceException("FundingSource can't be null!");
//                }
//
//                if (entryFundingSource.getFundingSource().getFundingSource() == null
//                        || entryFundingSource.getFundingSource().getFundingSource().isEmpty()) {
//                    throw new ServiceException("FundingSource can't be null or empty!");
//                }
//
//                if (entryFundingSource.getFundingSource().getPrincipalInvestigator() == null
//                        || entryFundingSource.getFundingSource().getPrincipalInvestigator()
//                                             .isEmpty()) {
//                    throw new ServiceException("PrincipalInvestigator can't be null or empty!");
//                }
//
//                boolean existEntryFundingSource = false;
//                for (EntryFundingSource currentEntryEntryFundingSource : currentEntry
//                        .getEntryFundingSources()) {
//
//                    if (currentEntryEntryFundingSource.getFundingSource().getFundingSource()
//                                                      .equals(entryFundingSource.getFundingSource()
// .getFundingSource())
//                            && currentEntryEntryFundingSource
//                            .getFundingSource()
//                            .getPrincipalInvestigator()
//                            .equals(entryFundingSource.getFundingSource().getPrincipalInvestigator())) {
//                        existEntryFundingSource = true;
//                        break;
//                    }
//                }
//
//                if (!existEntryFundingSource) {
//                    entryFundingSource.setEntry(currentEntry);
//                    currentEntry.getEntryFundingSources().add(entryFundingSource);
//                }
//
//                entryFundingSource.setEntry(entry);
//            }
//        }
//
//        return currentEntry;
//    }

    /**
     * Delete the specified {@link Entry} from the server.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the Entry.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public void deleteEntry(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") long entryId) throws SessionException, ServiceException {
        log(sessionId, "deleteEntry: " + entryId);
        Account account = validateAccount(sessionId);

        try {
            EntryController entryController = ControllerFactory.getEntryController();
            entryController.delete(account, entryId);
            log("User '" + account.getEmail() + "' removed entry: '" + entryId + "'");
        } catch (PermissionException e) {
            Logger.error(e);
            throw new ServiceException(e.getMessage());
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    /**
     * Get the {@link FeaturedDNASequence} of the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the desired Entry.
     * @return FeaturedDNASequence object.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public FeaturedDNASequence getSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "getSequence: " + entryId);
        Account account = validateAccount(sessionId);

        try {
            SequenceController sequenceController = ControllerFactory.getSequenceController();
            Entry entry = ControllerFactory.getEntryController().getByRecordId(account, entryId);
            FeaturedDNASequence sequence = sequenceController.sequenceToDNASequence(
                    sequenceController.getByEntry(entry));

            log("User '" + account.getEmail() + "' pulled sequence: '" + entryId + "'");
            return sequence;
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    @Override
    public FeaturedDNASequence getPublicSequence(@WebParam(name = "recordId") String recordId) throws ServiceException {
        log("getPublicSequence: " + recordId);
        try {
            return ControllerFactory.getEntryController().getPublicSequence(recordId);
        } catch (ControllerException ce) {
            throw new ServiceException(ce);
        }
    }

    /**
     * Retrieve the original uploaded sequence (Sequence.sequenceUser) of the specified
     * {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the desired Entry.
     * @return Content of the original uploaded sequence file as String.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public String getOriginalGenBankSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "getOriginalGenbankSequence: " + entryId);
        String genbankSequence = "";
        Account account = validateAccount(sessionId);

        try {
            SequenceController sequenceController = ControllerFactory.getSequenceController();
            EntryController entryController = ControllerFactory.getEntryController();
            Entry entry = entryController.getByRecordId(account, entryId);
            Sequence sequence = sequenceController.getByEntry(entry);

            if (sequence != null) {
                genbankSequence = sequence.getSequenceUser();
            }

            log("User '" + account.getEmail() + "' pulled original genbank sequence: '" + entryId + "'");
        } catch (PermissionException e) {
            throw new ServiceException(e);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return genbankSequence;
    }

    /**
     * Genbank formatted {@link Sequence} of the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the desired Entry.
     * @return Genbank file formatted string.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public String getGenBankSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "getGenBankSequence: " + entryId);
        String genbankSequence = "";
        Account account = validateAccount(sessionId);

        try {
            SequenceController sequenceController = ControllerFactory.getSequenceController();
            EntryController entryController = ControllerFactory.getEntryController();
            Entry entry = entryController.getByRecordId(account, entryId);
            Sequence sequence = sequenceController.getByEntry(entry);

            if (sequence != null) {
                GenbankFormatter genbankFormatter = new GenbankFormatter(entry.getName());
                genbankFormatter.setCircular((sequence.getEntry() instanceof Plasmid) ? ((Plasmid) entry)
                        .getCircular() : false);

                genbankSequence = sequenceController.compose(sequence, genbankFormatter);
            }

            log("User '" + account.getEmail() + "' pulled generated genbank sequence: '" + entryId + "'");
        } catch (PermissionException e) {
            throw new ServiceException(e);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return genbankSequence;
    }

    /**
     * Fasta formatted {@link Sequence} for the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the desired Entry.
     * @return Fasta formatted sequence.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public String getFastaSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "getFastaSequence: " + entryId);
        String fastaSequence = "";
        Account account = validateAccount(sessionId);

        try {
            SequenceController sequenceController = ControllerFactory.getSequenceController();
            EntryController entryController = ControllerFactory.getEntryController();
            Entry entry = entryController.getByRecordId(account, entryId);
            Sequence sequence = sequenceController.getByEntry(entry);

            if (sequence != null) {
                fastaSequence = sequenceController.compose(sequence, new FastaFormatter(entry.getName()));
            }

            log("User '" + account.getEmail() + "' pulled generated fasta sequence: '" + entryId + "'");
        } catch (PermissionException e) {
            throw new ServiceException(e);
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return fastaSequence;
    }

    /**
     * Assign the specified {@link Entry} a new {@link Sequence} object.
     *
     * @param sessionId           Session key.
     * @param entryId             RecordId of the desired Entry.
     * @param featuredDNASequence Annotated DNA Sequence.
     * @return {@link FeaturedDNASequence} as saved on the server.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public FeaturedDNASequence createSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId,
            @WebParam(name = "sequence") FeaturedDNASequence featuredDNASequence)
            throws SessionException, ServiceException {
        log(sessionId, "createSequence: " + entryId);
        Entry entry;
        FeaturedDNASequence savedFeaturedDNASequence;
        Account account = validateAccount(sessionId);

        try {
            EntryController entryController = ControllerFactory.getEntryController();
            SequenceController sequenceController = ControllerFactory.getSequenceController();
            entry = entryController.getByRecordId(account, entryId);

            if (entry == null) {
                throw new ServiceException("Entry doesn't exist!");
            }

            if (sequenceController.hasSequence(entry.getId())) {
                throw new ServiceException(
                        "Entry has sequence already assigned. Remove it first and then create new one.");
            }

            Sequence sequence = SequenceController.dnaSequenceToSequence(featuredDNASequence);
            sequence.setEntry(entry);

            savedFeaturedDNASequence = sequenceController
                    .sequenceToDNASequence(sequenceController.save(account, sequence));

            log("User '" + account.getEmail() + "' saved sequence: '" + entryId + "'");
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return savedFeaturedDNASequence;
    }

    /**
     * Remove the {@link Sequence} object associated with the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the entry.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public void removeSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "removeSequence: " + entryId);
        Account account = validateAccount(sessionId);

        try {
            EntryController entryController = ControllerFactory.getEntryController();
            Entry entry;

            try {
                entry = entryController.getByRecordId(account, entryId);
            } catch (PermissionException e) {
                throw new ServiceException("No permission to read this entry");
            }

            SequenceController sequenceController = ControllerFactory.getSequenceController();
            Sequence sequence = sequenceController.getByEntry(entry);

            if (sequence != null) {
                try {
                    sequenceController.delete(account, sequence);

                    log("User '" + account.getEmail() + "' removed sequence: '" + entryId + "'");
                } catch (PermissionException e) {
                    throw new ServiceException("No permission to delete sequence");
                }
            }
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    /**
     * Upload a sequence file (genbank, fasta, etc) and associate with the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the desired Entry.
     * @param sequence  Text of sequence file to parse.
     * @return {@link FeaturedDNASequence} object.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public FeaturedDNASequence uploadSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId, @WebParam(name = "sequence") String sequence)
            throws SessionException, ServiceException {
        log(sessionId, "uploadSequence: " + entryId);
        Account account = validateAccount(sessionId);
        EntryController entryController = ControllerFactory.getEntryController();
        SequenceController sequenceController = ControllerFactory.getSequenceController();
        FeaturedDNASequence dnaSequence = (FeaturedDNASequence) SequenceController.parse(sequence);

        if (dnaSequence == null) {
            throw new ServiceException("Couldn't parse sequence file! Supported formats: "
                                               + GeneralParser.getInstance().availableParsersToString());
        }

        Entry entry;

        FeaturedDNASequence savedFeaturedDNASequence;
        Sequence modelSequence;
        try {
            entry = entryController.getByRecordId(account, entryId);

            if (sequenceController.hasSequence(entry.getId())) {
                throw new ServiceException("Entry has sequence already assigned. Remove it and then upload new one.");
            }

            modelSequence = SequenceController.dnaSequenceToSequence(dnaSequence);
            modelSequence.setEntry(entry);
            modelSequence.setSequenceUser(sequence);
            Sequence savedSequence = sequenceController.save(account, modelSequence);
            savedFeaturedDNASequence = sequenceController.sequenceToDNASequence(savedSequence);
            log("User '" + account.getEmail() + "' uploaded new sequence: '" + entryId + "'");
        } catch (Exception e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        return savedFeaturedDNASequence;
    }

    /**
     * Retrieve all the {@link Sample}s of the specified {@link Entry}.
     *
     * @param sessionId Session key.
     * @param entryId   RecordId of the Entry.
     * @return List of Samples.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public ArrayList<Sample> retrieveEntrySamples(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        log(sessionId, "retrieveEntrySamples: " + entryId);
        Account account = validateAccount(sessionId);
        SampleController sampleController = ControllerFactory.getSampleController();
        EntryController entryController = ControllerFactory.getEntryController();

        try {
            Entry entry = entryController.getByRecordId(account, entryId);
            return sampleController.getSamples(entry);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServiceException("No permissions to view entry");
        }
    }

    /**
     * Retrieve the {@link Sample} object associated with a barcode.
     *
     * @param sessionId Session key.
     * @param barcode   Barcode string.
     * @return List of Samples.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public ArrayList<Sample> retrieveSamplesByBarcode(
            @WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "barcode") String barcode) throws SessionException, ServiceException {
        log(sessionId, "retrieveSamplesByBarcode: " + barcode);
        SampleController sampleController = ControllerFactory.getSampleController();
        StorageController storageController = ControllerFactory.getStorageController();

        try {
            Storage storage = storageController.retrieveStorageTube(barcode.trim());
            if (storage == null) {
                return null;
            }
            return sampleController.getSamplesByStorage(storage);
        } catch (ControllerException e) {
            Logger.error(e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Strain retrieveStrainForSampleBarcode(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "barcode") String barcode) throws SessionException, ServiceException {
        log(sessionId, "retrieveEntryForSampleBarcode: " + barcode);
        SampleController sampleController = ControllerFactory.getSampleController();
        StorageController storageController = ControllerFactory.getStorageController();

        try {
            Storage storage = storageController.retrieveStorageTube(barcode.trim());
            if (storage == null) {
                return null;
            }

            List<Sample> samples = sampleController.getSamplesByStorage(storage);
            for (Sample sample : samples) {
                Storage sampleStorage = sample.getStorage();
                if (sampleStorage == null)
                    continue;
                if (sampleStorage.getStorageType() != StorageType.TUBE)
                    continue;
                Entry entry = sample.getEntry();
                if (entry.getRecordType().equalsIgnoreCase(EntryType.STRAIN.toString()))
                    return (Strain) entry;
                return null;
            }
            return null;
        } catch (ControllerException e) {
            Logger.error(e);
            throw new ServiceException(e);
        }
    }

    /**
     * Checks if all samples have a common plate. If not, it determines which plate
     * is the most likely.
     *
     * @param sessionId id for session
     * @param samples   samples containing tube storage location with well parent
     * @return null if samples have a common plate, if not plate id (storage index) of most likely
     *         plate is returned
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public String samplePlate(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "samples") Sample[] samples) throws SessionException, ServiceException {
        log(sessionId, "samplePlate");
        StorageController storageController = ControllerFactory.getStorageController();
        HashMap<String, Integer> plateIndex = new HashMap<>();

        Sample initial = samples[0];
        Storage tube = initial.getStorage();

        // get tube by barcode and parent
        try {
            tube = storageController.retrieveStorageTube(tube.getIndex());
        } catch (ControllerException e) {
            Logger.error(e.getMessage());
            throw new ServiceException("Error retrieving storage location for tube " + tube.getIndex());
        }
        if (tube == null) {
            throw new ServiceException("Error retrieving storage location for tube");
        }

        Storage plate = tube.getParent().getParent();
        String highestFreqPlate = plate.getIndex();
        int highestFreqCount = 1;
        plateIndex.put(highestFreqPlate, highestFreqCount);

        for (int i = 1; i < samples.length; i += 1) {
            Sample sample = samples[i];
            String barcode = sample.getStorage().getIndex();
            if ("No Tube".equals(barcode) || "No Read".equals(barcode)) {
                continue;
            }

            try {
                tube = storageController.retrieveStorageTube(barcode);
            } catch (ControllerException e) {
                Logger.error(e.getMessage());
                throw new ServiceException("Error retrieving storage location for tube");
            }
            plate = tube.getParent().getParent();

            // check if this is new (not in plates map)
            Integer value = plateIndex.get(plate.getIndex());
            if (value == null) {
                // new
                plateIndex.put(plate.getIndex(), 1);
            } else {
                // update count
                value += 1;
                plateIndex.put(plate.getIndex(), value);
                if (value > highestFreqCount) {
                    highestFreqCount = value;
                    highestFreqPlate = plate.getIndex();
                }
            }
        }

        if (plateIndex.keySet().size() == 1) {
            return null;
        }

        return highestFreqPlate;
    }

    // Need moderator privileges to run this

    /**
     * Create a {@link Sample} object for the specified {@link Strain}.
     * <p/>
     * This assumes a plate->well->tube storage scheme.
     *
     * @param sessionId Session key.
     * @param recordId  RecordId of the Strain.
     * @param rack      -Rack number.
     * @param location  Location number.
     * @param barcode   Barcode number.
     * @param label     Label.
     * @throws ServiceException
     * @throws PermissionException
     * @throws SessionException
     */
    @Override
    public void createStrainSample(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "recordId") String recordId, @WebParam(name = "rack") String rack,
            @WebParam(name = "location") String location,
            @WebParam(name = "barcode") String barcode, @WebParam(name = "label") String label)
            throws ServiceException, PermissionException, SessionException {
        log(sessionId, "createStrainsample: " + recordId + "," + location + "," + barcode);
        Account account;
        AccountController controller = ControllerFactory.getAccountController();

        try {
            account = controller.getAccountBySessionKey(sessionId);
        } catch (ControllerException e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        try {
            if (!controller.isAdministrator(account)) {
                log("Account " + account.getEmail() + " attempting to access createStrainSample()");
                throw new PermissionException("Account does not have permissions");
            }
        } catch (ControllerException e) {
            log(e.getMessage());
            throw new ServiceException("Registry Service Internal Error!");
        }

        // check if there is an existing sample with barcode
        StorageController storageController = ControllerFactory.getStorageController();
        SampleController sampleController = ControllerFactory.getSampleController();

        try {
            Storage storage = storageController.retrieveStorageTube(barcode.trim());
            if (storage != null) {
                ArrayList<Sample> samples = sampleController.getSamplesByStorage(storage);
                if (samples != null && !samples.isEmpty()) {
                    log("Barcode \"" + barcode + "\" already has a sample associated with it");
                    return;
                }
            }
        } catch (ControllerException e) {
            Logger.error(e);
            throw new ServiceException(e);
        }

        log("Creating new strain sample for entry \"" + recordId + "\" and label \"" + label + "\"");
        // TODO : this is a hack till we migrate to a single strain default
        Storage strainScheme = null;
        try {
            List<Storage> schemes = storageController.retrieveAllStorageSchemes();
            for (Storage storage : schemes) {
                if (storage.getStorageType() == StorageType.SCHEME
                        && "Strain Storage Matrix Tubes".equals(storage.getName())) {
                    strainScheme = storage;
                    break;
                }
            }
            if (strainScheme == null) {
                log("Could not locate default strain scheme (Strain Storage Matrix Tubes[Plate, Well, Tube])");
                throw new ServiceException("Registry Service Internal Error!");
            }

            Storage newLocation = storageController.getLocation(strainScheme, new String[]{rack,
                    location, barcode
            });

            Entry entry = ControllerFactory.getEntryController().getByRecordId(account, recordId);
            if (entry == null) {
                throw new ServiceException("Could not retrieve entry with id " + recordId);
            }

            Sample sample = sampleController.createSample(label, account.getEmail(), "");
            sample.setEntry(entry);
            sample.setStorage(newLocation);
            Sample saved = sampleController.saveSample(account, sample);
            if (saved == null) {
                throw new ServiceException("Unable to create sample");
            }
        } catch (ControllerException ce) {
            log(ce.getMessage());
            throw new ServiceException(ce.getMessage());
        }
    }

    /**
     * Check and automagically update sample storage for the given
     *
     * @param sessionId Session key.
     * @param samples   Samples
     * @param plateId   plateId
     * @return List of {@link Sample}s.
     * @throws SessionException
     * @throws ServiceException
     */
    @Override
    public List<Sample> checkAndUpdateSamplesStorage(
            @WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "samples") Sample[] samples, @WebParam(name = "plateId") String plateId)
            throws SessionException, ServiceException {
        log(sessionId, "checkAndUpdateSamplesStorage: " + plateId);
        StorageController storageController = ControllerFactory.getStorageController();
        SampleController sampleController = ControllerFactory.getSampleController();

        // count of plates seen so far
        List<Sample> retSamples = new LinkedList<>();

        // for each sample (unique elements are the barcode (tube index))
        for (Sample sample : samples) {
            // sent storage location. note that barcode can be no read or no tube
            String barcode = sample.getStorage().getIndex();
            if ("No Tube".equals(barcode) || "No Read".equals(barcode)) {
                retSamples.add(sample);
                continue;
            }

            String location = sample.getStorage().getParent().getIndex();
            Storage recordedTube;

            try {
                // stored storage location
                recordedTube = storageController.retrieveStorageTube(barcode);
                Storage recordedWell = recordedTube.getParent();
                Storage recordedPlate = recordedWell.getParent();
                Storage parentScheme = recordedPlate.getParent();

                boolean samePlate = (plateId == null);
                boolean sameWell = recordedWell.getIndex().equals(location);

                if (samePlate) {
                    if (sameWell) {
                        ArrayList<Sample> ret = sampleController.getSamplesByStorage(recordedTube);
                        if (ret != null && !ret.isEmpty()) {
                            retSamples.add(ret.get(0));
                        }

                        continue; // no changes needed
                    } else {
                        // same plate but different well                        
                        Storage well = storageController.retrieveStorageBy("Well", location,
                                                                           StorageType.WELL, recordedPlate.getId());
                        if (well == null) {
                            throw new ServiceException(
                                    "Could not retrieve new location for storage");
                        }
                        recordedTube.setParent(well);
                        storageController.update(recordedTube);
                    }
                } else {
                    // different plate (update using the passed parameter)
                    Storage newPlate = storageController.retrieveStorageBy("Plate", plateId,
                                                                           StorageType.PLATE96, parentScheme.getId());
                    if (sameWell) {
                        // update plate only
                        recordedWell.setParent(newPlate);
                        storageController.update(recordedWell);

                    } else {
                        // update plate and well
                        Storage well = storageController.retrieveStorageBy("Well", location,
                                                                           StorageType.WELL, newPlate.getId());
                        recordedTube.setParent(well);
                        storageController.update(recordedTube);
                    }
                }
            } catch (ControllerException e) {
                Logger.error(e);
                throw new ServiceException("Error retrieving/updating some records!");
            }

            ArrayList<Sample> ret;
            try {
                ret = sampleController.getSamplesByStorage(recordedTube);
                if (ret != null && !ret.isEmpty()) {
                    retSamples.add(ret.get(0));
                }
            } catch (ControllerException e) {

                Logger.error(e);
                sample.setStorage(recordedTube);
                retSamples.add(sample);
            }

        }
        return retSamples;
    }

    /**
     * Retrieve a list of {@link TraceSequence}s associated with the specified entry.
     *
     * @param sessionId Session key.
     * @param recordId  RecordId of the desired Entry.
     * @return List of TraceSequences.
     * @throws ServiceException
     * @throws SessionException
     */
    @Override
    public List<TraceSequence> listTraceSequenceFiles(
            @WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "recordId") String recordId) throws ServiceException, SessionException {
        log(sessionId, "listTraceSequenceFiles: " + recordId);
        Account account = validateAccount(sessionId);
        List<TraceSequence> result = new ArrayList<>();
        SequenceAnalysisController sequenceAnalysisController = ControllerFactory.getSequenceAnalysisController();
        EntryController entryController = ControllerFactory.getEntryController();

        Entry entry;
        try {
            entry = entryController.getByRecordId(account, recordId);
            if (entry == null) {
                throw new ServiceException("Could not retrieve entry");
            }
        } catch (ControllerException e) {
            log(e.getMessage());
            throw new ServiceException("Could not retrieve entry: " + e.getMessage());
        } catch (PermissionException e) {
            log(e.getMessage());
            throw new ServiceException("No permission to view entry: " + recordId);
        }
        List<TraceSequence> traces;
        try {
            traces = sequenceAnalysisController.getTraceSequences(entry);
            if (traces == null) {
                return result;
            }
        } catch (ControllerException e) {
            throw new ServiceException("Could not retrieve traces: " + e.getMessage());
        }
        for (TraceSequence trace : traces) {
            //null out entry to reduce output.
            trace.setEntry(null);
            // null out traceSequenceAlignment.traceSequence, as it causes infinite nesting  in xml for some reason.
            if (trace.getTraceSequenceAlignment() != null)
                trace.getTraceSequenceAlignment().setTraceSequence(null);
            result.add(trace);
        }

        return result;
    }

    /**
     * Upload a sequence trace (abi) file.
     *
     * @param sessionId      Session key.
     * @param recordId       RecordId of the desired {@link Entry}.
     * @param fileName       Name of the trace file.
     * @param base64FileData Base64 encoded content of the file.
     * @return File ID as saved on the server.
     * @throws ServiceException
     * @throws SessionException
     */
    @Override
    public String uploadTraceSequenceFile(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "recordId") String recordId,
            @WebParam(name = "fileName") String fileName,
            @WebParam(name = "base64FileData") String base64FileData) throws ServiceException, SessionException {
        log(sessionId, "uploadTraceSequenceFile: " + recordId + "," + fileName);

        SequenceAnalysisController sequenceAnalysisController = ControllerFactory.getSequenceAnalysisController();
        EntryController entryController = ControllerFactory.getEntryController();
        byte[] bytes = SerializationUtils.deserializeBase64StringToBytes(base64FileData);
        if (bytes == null) {
            throw new ServiceException("Invalid File Data!");
        }
        Account account = validateAccount(sessionId);
        String depositor = account.getEmail();
        Entry entry;
        try {
            entry = entryController.getByRecordId(account, recordId);
            if (entry == null) {
                throw new ServiceException("Could not retrieve entry!");
            }
        } catch (ControllerException e1) {
            log(e1.getMessage());
            throw new ServiceException("Could not retrieve entry!");
        } catch (PermissionException e1) {
            log(e1.getMessage());
            throw new ServiceException("You do not have permission to view entry!");
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        String sequence;
        try {
            IDNASequence temp = sequenceAnalysisController.parse(bytes);

            if (temp == null) {
                throw new ServiceException("Could not parse trace file!");
            } else {
                sequence = temp.getSequence();
            }
        } catch (ControllerException e) {
            log(e.getMessage());
            throw new ServiceException("Could not parse trace file!: " + e.getMessage());
        }

        TraceSequence result;
        try {
            result = sequenceAnalysisController.uploadTraceSequence(entry, fileName, depositor, sequence, inputStream);
            sequenceAnalysisController.rebuildAllAlignments(entry);
        } catch (ControllerException e) {
            log(e.getMessage());
            throw new ServiceException("Could not upload trace sequence!: " + e.getMessage());
        }

        return result.getFileId();
    }

    /**
     * Retrieve the specified trace sequence file.
     *
     * @param sessionId Session Key.
     * @param fileId    File ID to retrieve.
     * @return {@link SequenceTraceFile} object.
     * @throws ServiceException
     * @throws SessionException
     */
    @Override
    public SequenceTraceFile getTraceSequenceFile(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "fileId") String fileId) throws ServiceException, SessionException {
        log(sessionId, "getTraceSequenceFile: " + fileId);
        SequenceAnalysisController sequenceAnalysisController = ControllerFactory.getSequenceAnalysisController();
        SequenceTraceFile traceFile;
        try {
            TraceSequence traceSequence = sequenceAnalysisController.getTraceSequenceByFileId(fileId);
            if (traceSequence == null) {
                throw new ServiceException("Could not retrieve Trace Sequence");
            }
            traceFile = sequenceAnalysisController.getSequenceTraceFile(traceSequence);

        } catch (ControllerException e) {
            log(e.getMessage());
            throw new ServiceException(e.getMessage());
        }

        return traceFile;
    }

    /**
     * Delete the specified trace sequence file.
     *
     * @param sessionId Session key.
     * @param fileId    ID of the file to delete.
     * @throws ServiceException
     * @throws SessionException
     */
    @Override
    public void deleteTraceSequenceFile(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "fileId") String fileId) throws ServiceException, SessionException {
        log(sessionId, "deleteTraceSequenceFile: " + fileId);
        Account account = validateAccount(sessionId);
        SequenceAnalysisController sequenceAnalysisController = ControllerFactory.getSequenceAnalysisController();

        TraceSequence traceSequence;
        try {
            traceSequence = sequenceAnalysisController.getTraceSequenceByFileId(fileId);
            if (traceSequence == null) {
                throw new ServiceException("No such fileId found");
            }
            Entry entry = traceSequence.getEntry();
            sequenceAnalysisController.removeTraceSequence(account, traceSequence);
            sequenceAnalysisController.rebuildAllAlignments(entry);
        } catch (ControllerException e) {
            log(e.getMessage());
            throw new ServiceException("Could not delete TraceSequence: " + e.getMessage());
        } catch (PermissionException e) {
            log(e.getMessage());
            throw new ServiceException("Deletion of this trace is not permitted");
        }
    }

    @Override
    public SearchResults runSearch(@WebParam(name = "searchQuery") SearchQuery query) throws ServiceException {
        Logger.info("Registry API: web search");
        SearchController controller = ControllerFactory.getSearchController();
        try {
            return controller.runLocalSearch(null, query, true);
        } catch (ControllerException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public void processWebOfRegistryPartnerInformation(@WebParam(name = "url") String uri,
            @WebParam(name = "partnerUrl") String name,
            @WebParam(name = "add") boolean add) throws ServiceException {
        Logger.info("API: processing web of registry partner " + name + "(" + uri + ")");
        WoRController controller = ControllerFactory.getWebController();
        try {
            if (add)
                controller.addWebPartner(uri, name);
            else
                controller.removeWebPartner(uri);
        } catch (ControllerException ce) {
            throw new ServiceException(ce);
        }
    }

    @Override
    public boolean transmitEntries(@WebParam(name = "entrySequenceMap") HashMap<Entry, String> entrySequenceMap)
            throws ServiceException {
        Logger.info("Registry API: transmit entries");
        ConfigurationController configurationController = ControllerFactory.getConfigurationController();
        boolean isWebEnabled;
        try {
            String value = configurationController.getPropertyValue(ConfigurationKey.JOIN_WEB_OF_REGISTRIES);
            isWebEnabled = value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
        } catch (ControllerException ce) {
            throw new ServiceException(ce);
        }

        if (!isWebEnabled)
            return false;

        EntryController controller = ControllerFactory.getEntryController();
        SequenceController sequenceController = ControllerFactory.getSequenceController();

        // process entries
        for (Map.Entry<Entry, String> mapEntry : entrySequenceMap.entrySet()) {
            Entry entry = mapEntry.getKey();

            try {
                entry = controller.recordEntry(entry, null);
            } catch (ControllerException e) {
                Logger.error(e);
                continue;
            }

            String sequenceString = mapEntry.getValue();
            if (sequenceString != null) {
                IDNASequence dnaSequence = SequenceController.parse(sequenceString);
                if (dnaSequence == null || dnaSequence.getSequence().equals("")) {
                    Logger.error("Couldn't parse sequence file!");
                }

                try {
                    Sequence sequence = SequenceController.dnaSequenceToSequence(dnaSequence);
                    sequence.setSequenceUser(sequenceString);
                    sequence.setEntry(entry);
                    sequenceController.saveSequence(sequence);
                } catch (ControllerException e) {
                    Logger.error(e);
                }
            }
        }

        return true;
    }

    /**
     * Retrieve the user {@link Account} associated with the given session key, if user is logged
     * in.
     *
     * @param sessionId Session key.
     * @return {@link Account} if user is logged in.
     * @throws ServiceException
     * @throws SessionException
     */
    protected Account validateAccount(@WebParam(name = "sessionId") String sessionId)
            throws ServiceException, SessionException {
        if (!isAuthenticated(sessionId)) {
            throw new SessionException("Unauthorized access! Authorize first!");
        }

        Account account;
        AccountController controller = ControllerFactory.getAccountController();

        try {
            account = controller.getAccountBySessionKey(sessionId);
        } catch (ControllerException e) {
            Logger.error(e);
            throw new ServiceException("Registry Service Internal Error!");
        }

        if (account == null) {
            Logger.error("Failed to lookup account!");
            throw new ServiceException("Registry Service Internal Error!");
        }

        return account;
    }

    /**
     * Write into the log at the INFO level, using the RegistryAPI prefix.
     *
     * @param message Log message.
     */
    private void log(String message) {
        Logger.info("RegistryAPI: " + message);
    }

    /**
     * Write into the log at the INFO level, using the RegistryAPI prefix and the account email
     * associated with the given session key.
     *
     * @param sessionId Session key.
     * @param message   Log message.
     */
    private void log(String sessionId, String message) {
        Account account;
        try {
            account = validateAccount(sessionId);
            if (account != null) {
                message = account.getEmail() + "\t" + message;
            }
        } catch (ServiceException e) {
            // it's ok, session expired.
            message = "invalid account\t" + message;
        } catch (SessionException e) {
            // It's ok, session expired.
            message = "invalid account\t" + message;
        }
        Logger.info("RegistryAPI: " + message);
    }
}
