package tests;


import applet.MainApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import org.junit.Assert;
import org.junit.jupiter.api.*;


import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

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
        final CommandAPDU cmd = new CommandAPDU(0xB0, 0x10, 0, 0);
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
