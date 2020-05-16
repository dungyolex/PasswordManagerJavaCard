package applet;

import javacard.framework.*;

public class MainApplet extends Applet
{
	/* Constants Declaration*/
	// code of CLA byte in the command APDU header
	final static byte Wallet_CLA =(byte)0xB0;
	// codes of INS byte in the command APDU heade
	//INS specifies the application instructions
	final static byte Deposit = (byte) 0x10;
	final static byte Debit = (byte) 0x20;
	final static byte Balance = (byte) 0x30;
	final static byte Validate = (byte) 0x40;

	//Max number of incorrect tries before pin block
	final static byte PinTryLimit = (byte) 0x03;
	//Max pin size
	final static byte MaxPinSize = (byte) 0x04;

	//Status word (SW1-SW2) to signal that the balance becomes negative
	final static short SW_NEGATIVE_BALANCE = (short) 0x6910;
	 /* instance variable declaration*/
	OwnerPIN pin;
	byte balance;
	//APDU buffer
	byte buffer[];

	private MainApplet(){
		pin = new OwnerPIN(PinTryLimit, MaxPinSize);
		balance = 0;
//		byte iLen = bArray[bOffset]; // aid length
//		bOffset = (short) (bOffset + iLen + 1);
//		byte cLen = bArray[bOffset]; // info length
//		bOffset = (short) (bOffset + cLen + 1);
//		byte aLen = bArray[bOffset]; // applet data length
		//Applet registers itself with JCRE by calling method, which is defined in class . Now the applet is visible to the outside world
		//pin.update(bArray, (short) (bOffset + 1), aLen);
		register();
	}

	/**
	 * Method is invoked by JCRE as the last step in the applet installation process
	 * @param
	 */
	public static void install(byte[] buffer, short offset, byte length){
		// The installation parameters contain the PIN initialization value
		new MainApplet();
	}

	/**
	 * This method is called by JCRE to inform that this applet has been selected. It performs necessary initialization which is required to process the following APDU messages.
	 * @return
	 */
	public boolean select(){
		// reset validation flag in the PIN object to false
		//pin.reset();
		// returns true to JCRE to indicate that the applet
		// is ready to accept incoming APDUs.
		return true;
	}

	/**
	 * After the applet is successfully selected, JCRE dispatches incoming APDUs to this method.
	 * APDU object is owned and maintained by JCRE. It encapsulates details of the underlying transmission protocol (T0 or T1 as specified in ISO 7816-3) by providing a common interface.
	 * @param apdu
	 */
	@Override
	public void process(APDU apdu) throws ISOException {
		// APDU object carries a byte array (buffer) to
		// transfer incoming and outgoing APDU header
		// and data bytes between card and CAD
		buffer = apdu.getBuffer();
		/**
		 * When an error occurs, the applet may decide to terminate the process and throw an exception containing status word (SW1 SW2) to indicate the processing state of the card.
		 * An exception that is not caught by an applet is caught by JCRE.
		 */
		// verify that if the applet can accept this
		// APDU message
		if(buffer[ISO7816.OFFSET_CLA]!=Wallet_CLA) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}

		/**The main function of method is to perform an action as specified in APDU and returns an appropriate response to the terminal.
		*INS byte specifies the type of action needs to be performed
		 * */
		switch (buffer[ISO7816.OFFSET_INS]){
			case Balance: getBalance(apdu);
				return;
			case Debit: debit(apdu);
				return;
			case Deposit: deposit(apdu);
				return;
			case Validate: validate(apdu);
				return;
			default:ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	/**
	 * The parameter APDU object contains a data field, which specifies the amount to be added onto the balance.
	 * Upon receiving the APDU object from JCRE, the first 5 bytes (CLA, INS, P1, P2, Lc/Le) are available in the APDU buffer. Their offsets in the APDU buffer are specified in the class ISO. Because the data field is optional, the applet needs to explicitly inform JCRE to retrieve additional data bytes.
	 * The communication between card and CAD is exchanged between command APDU and response APDU pair. In the deposit case, the response APDU contains no data field. JCRE would take the status word 0x9000 (normal processing) to form the correct response APDU. Applet developers do not need to concern the details of constructing the proper response APDU.
	 * When JCRE catches an Exception, which signals an error during processing the command, JCRE would use the status word contained in the Exception to construct the response APDU.
	 * @param apdu
	 */

	private void deposit(APDU apdu){
		if(!pin.isValidated()){
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
		// Lc byte denotes the number of bytes in the data
		// field of the comamnd APDU
		byte numBytes = (byte) (buffer[ISO7816.OFFSET_LC]);
		// indicate that this APDU has incoming data and
		// receive data starting from the offset
		// ISO.OFFSET_CDATA
		byte byteRead = (byte) (apdu.setIncomingAndReceive());
		//it is an error if the number of data bytes read does not match
		// match the number in the LC byte
		if(byteRead!=1){
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		// increase the balance by the amount specified in the data field of command APDU
		balance = (byte) (balance+buffer[ISO7816.OFFSET_CDATA]);
		//return successfully
		return;

	}

	/**
	 * In method, The APDU object contains a data field, which specifies the amount to be decrement from the balance
	 * @param apdu
	 */
	private void debit(APDU apdu){
		// access authentication
		if(!pin.isValidated()){
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
		byte numBytes = (byte)(buffer[ISO7816.OFFSET_LC]);
		byte byteRead = (byte) (apdu.setIncomingAndReceive());

		if(byteRead!=1){
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		//balance cannot be negative
		if((balance - buffer[ISO7816.OFFSET_CDATA])<0){
			ISOException.throwIt(SW_NEGATIVE_BALANCE);
		}
		balance = (byte) (balance-buffer[ISO7816.OFFSET_CDATA]);
	}

	/**
	 * returns the Wallets balance in the data field of the response APDU.
	 * Because the data field in response APDU is optional, the applet needs to explicitly inform JCRE of the additional data. JCRE uses the data array in the APDU object buffer and the proper status word to construct a complete response APDU.
	 */

	private void getBalance(APDU apdu){
		// access authentication
		if(!pin.isValidated()){
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
		// inform system that the applet has finished processing
		// the command and the system should now prepare to
		// construct a response APDU which contains data field
		apdu.setOutgoing();
		// indicate the number of bytes in the data field
		apdu.setOutgoingLength((byte)1);
		// move the data into the APDU buffer starting at offset 0
		buffer[0] = balance;
		// send 1 byte of data at offset 0 in the APDU buffer
		apdu.sendBytes((short)0, (short)1);

	}

	/**
	 * PIN is a method commonly used in smart cards to protect data from unauthorized access
	 * A PIN records the number of unsuccessful tries since the last correct PIN verification. The card would be blocked, if the number of unsuccessful tries exceeds the maximum number of allowed tries defined in the PIN.
	 * After the applet is successfully selected, PIN needs to be validated first, before any other instruction can be performed on the applet.
	 * @param apdu
	 */

	private void validate(APDU apdu){
		// retrieve the PIN data which requires to be validated
		// the user interface data is stored in the data field of the APDU
		byte byteRead = (byte) (apdu.setIncomingAndReceive());
		// object to be true if the validation succeeds.
		// if user interface validation fails, PinException would be
		// thrown from method.
		pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead);
	}

}
