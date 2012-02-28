package org.jbei.ice.client.collection.presenter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jbei.ice.client.collection.ICollectionEntriesView;
import org.jbei.ice.client.collection.event.SubmitEvent;
import org.jbei.ice.client.collection.event.SubmitHandler;
import org.jbei.ice.client.collection.view.OptionSelect;
import org.jbei.ice.client.common.entry.IHasEntryId;

public abstract class MoveToSubmitHandler implements SubmitHandler {

    private final ICollectionEntriesView view;
    private final IHasEntryId hasEntry;

    public MoveToSubmitHandler(ICollectionEntriesView view, IHasEntryId hasEntry) {
        this.view = view;
        this.hasEntry = hasEntry;
    }

    @Override
    public void onSubmit(SubmitEvent event) {
        List<OptionSelect> selected = view.getSelectedOptions(false);

        Set<Long> destinationFolders = new HashSet<Long>();

        for (OptionSelect option : selected) {
            destinationFolders.add(option.getId());
        }

        final ArrayList<Long> entryIds = new ArrayList<Long>(hasEntry.getEntrySet());

        // validate
        if (destinationFolders.isEmpty() || entryIds.isEmpty())
            return;

        // TODO : should be able to pass List<OptionSelect> without
        view.setBusyIndicator(destinationFolders);

        // service call to actually move
        moveEntriesToFolder(destinationFolders, entryIds);
    }

    protected abstract void moveEntriesToFolder(Set<Long> destinationFolders,
            ArrayList<Long> entryIds);
}
