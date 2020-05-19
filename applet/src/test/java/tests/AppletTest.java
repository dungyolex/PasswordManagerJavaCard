package tests;


import applet.MainApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import org.junit.Assert;
import org.junit.jupiter.api.*;


import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.xml.bind.DatatypeConverter;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 */
public class AppletTest extends BaseTest{
    
    public AppletTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUpMethod() throws Exception {
    }

    @AfterEach
    public void tearDownMethod() throws Exception {
    }

    // Is Card Working
    @Test
    public void pingTest() throws Exception {
        //String apdu = "00300000F104486F6D65F203626F62F30470617373";
        int cla = 0x00;
        int ins = 0xA2;
        int p1  = 0x00;
        int p2  = 0x00;
        //int LC  = 0x08;'
        // F1: Identifier, F2: Username, F3: password
        byte[] data = new byte[] {(byte) 0xF1, (byte) 0x04, (byte) 0x48, (byte) 0x6F, (byte) 0x6D, (byte) 0x65,
                                  (byte) 0xF2, (byte) 0x03, (byte) 0x62, (byte) 0x6F, (byte) 0x62,
                                  (byte) 0xF3, (byte) 0x04, (byte) 0x70, (byte) 0x61, (byte) 0x73, (byte) 0x73,};
        //int le  = 0x0D;
        final CommandAPDU cmd = new CommandAPDU(cla, ins, p1, p2, data);
        final ResponseAPDU responseAPDU = connect().transmit(cmd);
        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(0x9000, responseAPDU.getSW());
        Assert.assertNotNull(responseAPDU.getBytes());

        //Card Simulator is not recommended
//        // 1. Create simulator
//        CardSimulator simulator = new CardSimulator();
//
//// 2. Install applet
//        AID appletAID = AIDUtil.create(BaseTest.APPLET_AID);
//        simulator.installApplet(appletAID, MainApplet.class);
//
//// 3. Select applet
//        simulator.selectApplet(appletAID);
//
//// 4. Send APDU
//        CommandAPDU commandAPDU = new CommandAPDU(0x00, 0x01, 0x00, 0x00);
//        ResponseAPDU response = simulator.transmitCommand(commandAPDU);
//
//// 5. Check response status word
//        Assert.assertEquals(0x9000, response.getSW());
    }

    public static byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    @Test
    private void creditTest(){

    }

    @Test
    private void debitTest(){

    }

    @Test
    private void showBalanceTest(){

    }

    @Test
    private void validateTest(){

    }
}
