package gurux.serial.dlms;

import android.content.Context;
import android.widget.Toast;

import gurux.common.GXCommon;
import gurux.common.ReceiveParameters;
import gurux.dlms.GXByteBuffer;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDLMSException;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.DataType;
import gurux.dlms.enums.InterfaceType;
import gurux.dlms.enums.ObjectType;
import gurux.dlms.manufacturersettings.GXAttributeCollection;
import gurux.dlms.objects.GXDLMSObjectCollection;
import gurux.serial.GXSerial;

public class DlmsJavaClient {

    GXDLMSClient client;

    Context mCtx;

    GXSerial Media;

    private static DlmsJavaClient INSTANCE = null;

    public byte[][] getData() {
        return client.write("0.0.1.0.0.255", java.util.Calendar.getInstance().getTime(), DataType.OCTET_STRING, ObjectType.CLOCK, 2);
    }

    private DlmsJavaClient(GXSerial media, Context ctx) {
        client = new GXDLMSClient();
        this.Media = media;
        this.mCtx = ctx;
        setInitialProps();
    }

    public static DlmsJavaClient newInstance(GXSerial media, Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new DlmsJavaClient(media, ctx);
        }
        return INSTANCE;
    }

    private final void setInitialProps() {
        client.setUseLogicalNameReferencing(true);
        client.setInterfaceType(InterfaceType.HDLC);
        client.setClientAddress(16);
        client.setServerAddress(1);
        //client.setAuthentication(Authentication.HIGH_MD5);
        //client.setPassword("A1B2C3D4".getBytes());
    }

    public void disconnect() throws Exception {
        readDLMSPacket(client.disconnectRequest());
        Media.close();
    }

    /*
     * Read DLMS Data from the device.
     * If access is denied return null.
     */
    public byte[] readDLMSPacket(byte[] data)
            throws Exception {
        GXReplyData reply = new GXReplyData();
        if (data == null || data.length == 0) {
            return null;
        }
        Object eop = (byte) 0x7E;
        // In network connection terminator is not used.
        /*if (client.getInterfaceType() == InterfaceType.WRAPPER/*
                && Media instanceof GXNet) {
            eop = null;
        }*/
        Integer pos = 0;
        boolean succeeded = false;
        ReceiveParameters<byte[]> p =
                new ReceiveParameters<byte[]>(byte[].class);
        p.setAllData(true);
        p.setEop(eop);
        p.setCount(5);
        p.setWaitTime(5000);
        synchronized (Media.getSynchronous()) {
            while (!succeeded) {
                Media.send(data, null);
                if (p.getEop() == null) {
                    p.setCount(1);
                }
                Toast.makeText(this.mCtx, "Media sent some data : " + GXCommon.bytesToHex(data), Toast.LENGTH_LONG).show();
                succeeded = Media.receive(p);
                if (!succeeded) {
                    Toast.makeText(this.mCtx, "Not Succeeded receiving data", Toast.LENGTH_LONG).show();
                    // Try to read again...
                    if (pos++ != 3) {
                        Toast.makeText(this.mCtx, "Data send failed. Try to resend "
                                + pos.toString() + "/3", Toast.LENGTH_LONG).show();
                        continue;
                    }
                    throw new RuntimeException(
                            "Failed to receive reply from the device in given time.");
                }
            }

            client.getData(p.getReply(), reply);
            // Loop until whole DLMS packet is received.
            while (!reply.isComplete()) {
                if (p.getEop() == null) {
                    p.setCount(1);
                }
                if (!Media.receive(p)) {
                    throw new Exception(
                            "Failed to receive reply from the device in given time.");
                }
            }
        }
        if (reply.getError() != 0) {
            throw new GXDLMSException(reply.getError());
        }
        return reply.getData().array();
    }

    public GXDLMSObjectCollection getMeterObjects() {
        return client.getObjects();
    }

    public void doHandshake() throws Exception {
        byte[] data, reply = null;
        data = client.snrmRequest();
        if (data != null)
        {
            reply = this.readDLMSPacket("06 30 30 30 0D 0A".getBytes());
            Toast.makeText(this.mCtx, "Baud Rate Set here :: " + GXCommon.bytesToHex(reply), Toast.LENGTH_LONG).show();
            reply = this.readDLMSPacket(data);
            Toast.makeText(this.mCtx, "SNRM Request :: " + GXCommon.bytesToHex(reply), Toast.LENGTH_LONG).show();
            //Has server accepted client.
            client.parseUAResponse(reply);
        }

        //Generate AARQ request.
        //Split requests to multiple packets if needed.
        //If password is used all data might not fit to one packet.
        for (byte[] it : client.aarqRequest())
        {
            reply = readDLMSPacket(it);
            client.parseAareResponse(new GXByteBuffer(reply));
            Toast.makeText(this.mCtx, "AARQ Request :: " + GXCommon.bytesToHex(reply), Toast.LENGTH_LONG).show();
        }
        if (client.getIsAuthenticationRequired())
        {
            for (byte[] array : client.getApplicationAssociationRequest()) {
                reply = readDLMSPacket(array);
                client.parseApplicationAssociationResponse(new GXByteBuffer(reply));
            }
        }
    }
}
