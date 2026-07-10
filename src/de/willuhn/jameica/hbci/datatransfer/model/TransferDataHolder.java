package de.willuhn.jameica.hbci.datatransfer.model;

import de.willuhn.jameica.hbci.rmi.Konto;

/**
 * Hält TransferData und Konto zusammen für die Übergabe an die Views.
 */
public class TransferDataHolder {

    private final TransferData transferData;
    private final Konto konto;

    public TransferDataHolder(TransferData transferData, Konto konto) {
        this.transferData = transferData;
        this.konto = konto;
    }

    public TransferData getTransferData() {
        return transferData;
    }

    public Konto getKonto() {
        return konto;
    }
}
