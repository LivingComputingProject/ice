package org.jbei.ice.client.bulkimport;

import java.util.ArrayList;

import org.jbei.ice.client.admin.bulkimport.BulkImportMenuItem;
import org.jbei.ice.client.admin.bulkimport.IDeleteMenuHandler;
import org.jbei.ice.client.bulkimport.model.NewBulkInput;
import org.jbei.ice.shared.EntryAddType;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;

public interface IBulkImportView {

    void setHeader(String header);

    Widget asWidget();

    void showFeedback(String msg, boolean isError);

    void setSavedDraftsData(ArrayList<BulkImportMenuItem> data, IDeleteMenuHandler handler);

    void addSavedDraftData(BulkImportMenuItem item, IDeleteMenuHandler handler);

    SingleSelectionModel<BulkImportMenuItem> getDraftMenuModel();

    SingleSelectionModel<EntryAddType> getImportCreateModel();

    void setDraftMenuVisibility(boolean visible);

    void addToggleMenuHandler(ClickHandler handler);

    boolean getMenuVisibility();

    void setToggleMenuVisiblity(boolean visible);

    void setSheet(NewBulkInput input, boolean isNew);

    HandlerRegistration setDraftUpateHandler(ClickHandler handler);

    String getDraftName();

    void setSubmitHandler(ClickHandler submitHandler);

    void setResetHandler(ClickHandler resetHandler);

    void setDraftSaveHandler(ClickHandler draftSaveHandler);
}
