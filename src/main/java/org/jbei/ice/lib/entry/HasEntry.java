package org.jbei.ice.lib.entry;

import org.jbei.ice.storage.DAOFactory;
import org.jbei.ice.storage.hibernate.dao.EntryDAO;
import org.jbei.ice.storage.model.Entry;

/**
 * Parent class for all objects that have an entry, or need to retrieve one
 * Provides a means to retrieve an entry using an id that can either be
 * the database identifier for the entry object or any one of the other unique entry
 * fields. e.g. <code>part number</code> or <code>universally unique id</code>
 * <p>
 * Also provides access to the entry data accessor object
 *
 * @author Hector Plahar
 */
public class HasEntry {

    protected final EntryDAO entryDAO;

    public HasEntry() {
        this.entryDAO = DAOFactory.getEntryDAO();
    }

    protected Entry getEntry(String id) {
        Entry entry = null;

        // check if numeric
        try {
            entry = entryDAO.get(Long.decode(id));
        } catch (NumberFormatException nfe) {
            // fine to ignore
        }

        // check for part Id
        if (entry == null)
            entry = entryDAO.getByPartNumber(id);

        // check for global unique id
        if (entry == null)
            entry = entryDAO.getByRecordId(id);

        return entry;
    }
}
