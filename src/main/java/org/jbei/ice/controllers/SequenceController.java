package org.jbei.ice.controllers;

import java.util.ArrayList;

import org.biojava.utils.ParserException;
import org.jbei.ice.controllers.common.Controller;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.controllers.permissionVerifiers.SequencePermissionVerifier;
import org.jbei.ice.lib.composers.SequenceComposer;
import org.jbei.ice.lib.composers.SequenceComposerException;
import org.jbei.ice.lib.composers.formatters.IFormatter;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.managers.SequenceManager;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.parsers.GeneralParser;
import org.jbei.ice.lib.permissions.PermissionException;

public class SequenceController extends Controller {
    public SequenceController(Account account) {
        super(account, new SequencePermissionVerifier());
    }

    public boolean hasReadPermission(Sequence sequence) {
        return getSequencePermissionVerifier().hasReadPermissions(sequence, getAccount());
    }

    public boolean hasWritePermission(Sequence sequence) {
        return getSequencePermissionVerifier().hasWritePermissions(sequence, getAccount());
    }

    public Sequence getByEntry(Entry entry) throws ControllerException {
        Sequence sequence = null;

        try {
            sequence = SequenceManager.getByEntry(entry);
        } catch (ManagerException e) {
            throw new ControllerException(e);
        }

        return sequence;
    }

    public Sequence save(Sequence sequence) throws ControllerException, PermissionException {
        Sequence result = null;

        if (sequence == null) {
            throw new ControllerException("Failed to save null sequence!");
        }

        if (!hasWritePermission(sequence)) {
            throw new PermissionException("No write permission for sequence!");
        }

        // TODO: Use transactional saveSequence
        try {
            result = SequenceManager.saveSequence(sequence);
        } catch (ManagerException e) {
            throw new ControllerException(e);
        }

        return result;
    }

    public Sequence update(Sequence sequence) throws ControllerException, PermissionException {
        Sequence result = null;

        if (sequence == null) {
            throw new ControllerException("Failed to save null sequence!");
        }

        if (!hasWritePermission(sequence)) {
            throw new PermissionException("No write permission for sequence!");
        }

        try {
            // TODO: Use transactions here in case delete OK but save fails
            // TODO: Refactor this method
            Entry entry = sequence.getEntry();
            Sequence oldSequence = entry.getSequence();
            entry.setSequence(null);

            if (oldSequence != null) {
                SequenceManager.deleteSequence(oldSequence);
            }

            sequence.setEntry(entry);
            entry.setSequence(sequence);

            result = SequenceManager.saveSequence(sequence);
        } catch (ManagerException e) {
            throw new ControllerException(e);
        }

        return result;
    }

    public void delete(Sequence sequence) throws ControllerException, PermissionException {
        if (sequence == null) {
            throw new ControllerException("Failed to save null sequence!");
        }

        if (!hasWritePermission(sequence)) {
            throw new PermissionException("No write permission for sequence!");
        }

        try {
            SequenceManager.deleteSequence(sequence);
        } catch (ManagerException e) {
            throw new ControllerException(e);
        }
    }

    public ArrayList<Sequence> getSequences() throws ControllerException {
        ArrayList<Sequence> sequences = new ArrayList<Sequence>();

        try {
            ArrayList<Sequence> allSequences = SequenceManager.getSequences();

            for (Sequence sequence : allSequences) {
                if (hasReadPermission(sequence)) {
                    sequences.add(sequence);
                }
            }
        } catch (ManagerException e) {
            throw new ControllerException(e);
        }

        return sequences;
    }

    public Sequence parse(String sequence) throws ParserException {
        return GeneralParser.getInstance().parse(sequence);
    }

    public String compose(Sequence sequence, IFormatter formatter) throws SequenceComposerException {
        return SequenceComposer.compose(sequence, formatter);
    }

    protected SequencePermissionVerifier getSequencePermissionVerifier() {
        return (SequencePermissionVerifier) getPermissionVerifier();
    }
}