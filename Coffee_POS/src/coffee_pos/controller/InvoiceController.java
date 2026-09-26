package coffee_pos.controller;

import javafx.print.PrinterJob;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class InvoiceController {

    @FXML
    private TextArea invoiceTextArea;

    public void setInvoiceText(String invoiceText) {
        invoiceTextArea.setText(invoiceText);
    }

    @FXML
    private void handlePrintToConsole() {
        PrinterJob job = PrinterJob.createPrinterJob();

    if (job != null && job.showPrintDialog(invoiceTextArea.getScene().getWindow())) {
        boolean success = job.printPage(invoiceTextArea);
        if (success) {
            job.endJob(); // Finalize the printing
        }
    }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) invoiceTextArea.getScene().getWindow();
        stage.close();
    }
}
