package org.jbei.ice.client.collection.add.form;

import org.jbei.ice.client.common.widget.MultipleTextBox;
import org.jbei.ice.lib.shared.EntryAddType;
import org.jbei.ice.lib.shared.dto.entry.AutoCompleteField;
import org.jbei.ice.lib.shared.dto.entry.PlasmidData;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Form for creating a new plasmid
 *
 * @author Hector Plahar
 */

public class PlasmidForm extends EntryForm<PlasmidData> {

    private CheckBox circular;
    private TextBox backbone;
    private SuggestBox markers;
    private SuggestBox origin;
    private SuggestBox promoters;
    private SuggestBox replicatesIn;

    public PlasmidForm(PlasmidData data) {
        super(data);

        circular.setValue(data.getCircular());
        backbone.setText(data.getBackbone());
        origin.setText(data.getOriginOfReplication());
        promoters.setText(data.getPromoters());
        replicatesIn.setText(data.getReplicatesIn());
        markers.setText(data.getSelectionMarkers());
    }

    protected void addField(FlexTable table, String label, int row, int col, TextBox box,
            String help, boolean required) {
        setLabel(required, label, table, row, col);
        if (help != null) {
            Widget widget = createTextBoxWithHelp(box, help);
            table.setWidget(row, col + 1, widget);
        } else
            table.setWidget(row, col + 1, box);
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        markers = createSuggestBox(AutoCompleteField.SELECTION_MARKERS, "300px");
        circular = new CheckBox();
        origin = createSuggestBox(AutoCompleteField.ORIGIN_OF_REPLICATION, "300px");
        promoters = createSuggestBox(AutoCompleteField.PROMOTERS, "300px");
        replicatesIn = createSuggestBox(AutoCompleteField.REPLICATES_IN, "300px");
        backbone = createStandardTextBox("300px", 150);
    }

    protected Widget createGeneralWidget() {
        int row = 0;
        FlexTable general = new FlexTable();
        general.setWidth("100%");
        general.setCellPadding(2);
        general.setStyleName("no_wrap");
        general.setCellSpacing(0);

        // name
        addField(general, "Name", row, 0, name, "e.g. pTSH117", true);

        // alias
        addField(general, "Alias", row, 2, alias, null, false);

        // creator
        row += 1;
        setLabel(true, "Creator", general, row, 0);
        Widget widget = createTextBoxWithHelp(creator, "Who made this part?");
        general.setWidget(row, 1, widget);

        // PI
        setLabel(true, "Principal Investigator", general, row, 2);
        general.setWidget(row, 3, principalInvestigator);

        // creator's email
        row += 1;
        setLabel(false, "Creator's Email", general, row, 0);
        widget = createTextBoxWithHelp(creatorEmail, "If known");
        general.setWidget(row, 1, widget);

        // funding source
        setLabel(false, "Funding Source", general, row, 2);
        general.setWidget(row, 3, fundingSource);

        // status
        row += 1;
        setLabel(false, "Status", general, row, 0);
        general.setWidget(row, 1, status);

        // bio safety level
        setLabel(false, "Bio Safety Level", general, row, 2);
        general.setWidget(row, 3, bioSafety);

        // circular
        row += 1;
        setLabel(false, "Circular", general, row, 0);
        circular.setValue(true);
        general.setWidget(row, 1, circular);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // backbone
        row += 1;
        setLabel(false, "Backbone", general, row, 0);
        general.setWidget(row, 1, backbone);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // links
        row += 1;
        setLabel(false, "Links", general, row, 0);
        widget = createTextBoxWithHelp(links, "Comma separated");
        general.setWidget(row, 1, widget);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // selection markers
        row += 1;
        setLabel(true, "Selection Markers", general, row, 0);
        widget = createTextBoxWithHelp(markers, "Comma separated");
        general.setWidget(row, 1, widget);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // origin of replication
        row += 1;
        setLabel(false, "Origin of Replication", general, row, 0);
        widget = createTextBoxWithHelp(origin, "Comma separated");
        general.setWidget(row, 1, widget);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // promoters
        row += 1;
        setLabel(false, "Promoters", general, row, 0);
        widget = createTextBoxWithHelp(promoters, "Comma separated");
        general.setWidget(row, 1, widget);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // replicates In
        row += 1;
        setLabel(false, "Replicates In", general, row, 0);
        general.setWidget(row, 1, replicatesIn);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // keywords
        row += 1;
        setLabel(false, "Keywords", general, row, 0);
        general.setWidget(row, 1, keywords);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // summary
        row += 1;
        setLabel(true, "Summary", general, row, 0);
        general.setWidget(row, 1, summary);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // references
        row += 1;
        setLabel(false, "References", general, row, 0);
        general.setWidget(row, 1, references);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        // intellectual property
        row += 1;
        setLabel(false, "Intellectual Property", general, row, 0);
        general.setWidget(row, 1, ip);
        general.getFlexCellFormatter().setColSpan(row, 1, 3);

        return general;
    }

    @Override
    public void populateEntries() {
        super.populateEntries();

        // plasmid specific fields
        PlasmidData data = super.getEntryInfo();

        String selectionMarkers = ((MultipleTextBox) markers.getValueBox()).getWholeText();
        data.setSelectionMarkers(selectionMarkers);
        data.setBackbone(this.backbone.getText());
        data.setOriginOfReplication(((MultipleTextBox) origin.getValueBox()).getWholeText());
        data.setPromoters(((MultipleTextBox) promoters.getValueBox()).getWholeText());
        data.setReplicatesIn(replicatesIn.getText());
        data.setCircular(this.circular.getValue());
    }

    @Override
    public FocusWidget validateForm() {
        FocusWidget widget = super.validateForm();
        if (markers.getValueBox().getText().trim().isEmpty()) {
            markers.setStyleName("input_box_error");
            if (widget == null)
                widget = markers.getValueBox();
        } else {
            markers.setStyleName("input_box");
        }
        return widget;
    }

    @Override
    public String getHeaderDisplay() {
        return EntryAddType.PLASMID.getDisplay();
    }
}
