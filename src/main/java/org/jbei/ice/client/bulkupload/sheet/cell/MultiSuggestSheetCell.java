package org.jbei.ice.client.bulkupload.sheet.cell;

import java.util.ArrayList;

import org.jbei.ice.client.bulkupload.model.SheetCellData;
import org.jbei.ice.client.common.widget.MultipleTextBox;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Hector Plahar
 */
public class MultiSuggestSheetCell extends SheetCell {

    protected final MultiWordSuggestOracle oracle;
    protected final SuggestBox box;
    protected final TextBoxBase textBox;
    private final ArrayList<String> oracleData = new ArrayList<String>();
    private int currentRow;
    private final boolean commaSeparatedAllowed;

    public MultiSuggestSheetCell(boolean commaSeparatedAllowed) {
        super();

        oracle = new MultiWordSuggestOracle();
        this.commaSeparatedAllowed = commaSeparatedAllowed;
        if (commaSeparatedAllowed)
            textBox = new MultipleTextBox();
        else
            textBox = new TextBox();
        box = new SuggestBox(oracle, textBox);
        box.setStyleName("cell_input");

        textBox.addBlurHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent event) {
                String s = setDataForRow(currentRow);
                textBox.setText(s);
            }
        });
    }

    public MultiSuggestSheetCell(ArrayList<String> data, boolean commaSeparatedAllowed) {
        this(commaSeparatedAllowed);
        oracle.addAll(data);
        oracleData.addAll(data);
    }

    @Override
    public void setText(String text) {
        box.setText(text);
    }

    /**
     * Sets data for row specified in the param
     *
     * @param row current row user is working on
     * @return display for user entered value
     */
    @Override
    public String setDataForRow(int row) {
        String ret;
        if (commaSeparatedAllowed)
            ret = ((MultipleTextBox) textBox).getWholeText();
        else
            ret = textBox.getText();
        SheetCellData data = new SheetCellData();
        data.setId(ret);
        data.setValue(ret);
        setWidgetValue(row, data);
        box.setText("");
        return ret;
    }

    @Override
    public void setFocus(int row) {
        textBox.setFocus(true);
        currentRow = row;
    }

    /**
     * Adds the suggestions that will be presented to user to oracle
     *
     * @param data list of strings presented to user
     */
    public void addOracleData(ArrayList<String> data) {
        oracle.clear();
        oracle.addAll(data);
        oracleData.clear();
        oracleData.addAll(data);
    }

    public boolean hasMultiSuggestions() {
        return true;
    }

    @Override
    public Widget getWidget(int row, boolean isCurrentSelection, int tabIndex) {
        box.setTabIndex(tabIndex);
        return box;
    }
}
