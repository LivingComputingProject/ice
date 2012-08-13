package org.jbei.ice.client.bulkupload.sheet.cell;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;

/**
 * @author Hector Plahar
 */
public class DateInputCell extends SheetCell {

    private DateBox dateBox;

    public DateInputCell() {

        dateBox = new DateBox();
        dateBox.setStyleName("cell_input");
        dateBox.getDatePicker().setStyleName("font-70em");

        DateTimeFormat dateFormat = DateTimeFormat.getFormat("MM/dd/yyyy");
        dateBox.setFormat(new DateBox.DefaultFormat(dateFormat));
    }

    /**
     * Set text value of input widget; typically text box base. This is used when focus shifts from
     * displaying the label to the actual widget. This gives the widget the chance to set their text
     * (which is the existing)
     *
     * @param text value to set
     */
    @Override
    public void setText(String text) {
    }

    /**
     * Sets data for row specified in the param, using the user entered value in the input widget
     *
     * @param row current row user is working on
     * @return display for user entered value
     */
    @Override
    public String setDataForRow(int row) {

        String ret = dateBox.getTextBox().getText();
        setWidgetValue(row, ret, ret);
        dateBox.getTextBox().setText("");
        return ret;
    }

    /**
     * Give focus to the widget that is wrapped by this cell
     *
     * @param row index of row for focus
     */
    @Override
    public void setFocus(int row) {
        dateBox.showDatePicker();
    }

    @Override
    public Widget getWidget(int row, boolean isCurrentSelection) {
        return dateBox;
    }
}
