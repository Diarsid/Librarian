package diarsid.librarian.impl.logic.impl;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import diarsid.librarian.api.Core;
import diarsid.librarian.impl.logic.api.UuidSupplier;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;

import static diarsid.librarian.api.Core.Mode.DEVELOPMENT;

public class SequentialUuidTimeBasedMACSupplierImpl implements UuidSupplier {

    private final AtomicReference<Core.Mode> coreMode;
    private final TimeBasedGenerator uuidGenerator;

    public SequentialUuidTimeBasedMACSupplierImpl(AtomicReference<Core.Mode> coreMode) {
        this.coreMode = coreMode;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            EthernetAddress address = null;
            while ( networkInterfaces.hasMoreElements() && isNull(address) ) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if ( nonNull(mac) ) {
                    address = new EthernetAddress(mac);
                    System.out.println(address.toString());
                }
            }

            this.uuidGenerator = Generators.timeBasedGenerator(address);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID nextRandomUuid() {
        if ( this.coreMode.get().equalTo(DEVELOPMENT) ) {
            return randomUUID();
        }
        else {
            return this.uuidGenerator.generate();
        }
    }
}
