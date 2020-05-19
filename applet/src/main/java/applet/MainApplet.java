package applet;

import javacard.framework.*;

public class MainApplet extends Applet
{
	/** INS for command that adds a password entry */
	public final static byte INS_ADD_PASSWORD_ENTRY = (byte) 0x30 ;

	/** INS for command that retrieves a password entry */
	public final static byte INS_RETRIEVE_PASSWORD_ENTRY = (byte) 0x32 ;

	/** INS for command that deletes a password entry */
	public final static byte INS_DELETE_PASSWORD_ENTRY = (byte) 0x34 ;

	/** INS for command that lists all defined identifiers */
	public final static byte INS_LIST_IDENTIFIERS = (byte) 0x36 ;

	/** INS byte for the command that verifies the PIN
	 * (from ISO7816-4) */
	public final static byte INS_VERIFY = (byte) 0x20 ;

	/** INS byte for the command that changes the PIN
	 * (from ISO7816-4) */
	public final static byte INS_CHANGE_REFERENCE_DATA = (byte) 0x24 ;

	/** Status word for a duplicate identifier */
	public final static short SW_DUPLICATE_IDENTIFIER = (short) 0x6A8A ;

	public final static short SW_IDENTIFIER_NOT_FOUND  = (short) 0x6A8B;

	/** Status word for a failed allocation */
	public final static short SW_NOT_ENOUGH_MEMORY = (short) 0x6A84 ;

	//The final ste of constants are related the tags used to structure data:

	/** Tag byte for identifiers */
	public final static byte TAG_IDENTIFIER = (byte) 0xF1 ;

	/** Tag byte for user name records */
	public final static byte TAG_USERNAME = (byte) 0xF2 ;

	/** Tag byte for password records */
	public final static byte TAG_PASSWORD = (byte) 0xF3 ;

	private PasswordEntry current ;
	private OwnerPIN pin ;
	/** PIN try limit */
	public final static byte PIN_TRY_LIMIT = (byte) 3 ;

	/** PIN Maximum size */
	public final static byte PIN_MAX_SIZE = (byte) 16 ;

	private MainApplet(){
		pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_MAX_SIZE) ;
	}

	/**
	 * This code just does exactly what I described. It uses as registration AID the AID that was sent in the Install [for install]
	 * command, assuming that the application is being installed in a GlobalPlatform-compliant card.
	 * @param
	 */
	public static void install(byte[] buffer, short offset, byte length) throws ISOException{
		// The installation parameters contain the PIN initialization value
		(new MainApplet()).register(buffer, (short)(offset+1), buffer[offset]);
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
	 * Resets the PIN's validated flag upon deselection of the applet.
	 */
	public void deselect()
	{
		pin.reset();
	}

	/**
	 * After the applet is successfully selected, JCRE dispatches incoming APDUs to this method.
	 * APDU object is owned and maintained by JCRE. It encapsulates details of the underlying transmission protocol (T0 or T1 as specified in ISO 7816-3) by providing a common interface.
	 * @param apdu
	 */
	@Override
	public void process(APDU apdu) throws ISOException {
		// APDU object carries a byte array (buffer) to
		if(selectingApplet()){
			return;
		}

		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS]){
			case INS_ADD_PASSWORD_ENTRY:
				processAddPassword(buf);
				break;
			case INS_RETRIEVE_PASSWORD_ENTRY:
				processRetrivePasswordEntry(buf);
				break;
//			case INS_DELETE_PASSWORD_ENTRY:
//				processDeletePasswordEntry();
//				break;
//			case INS_LIST_IDENTIFIERS:
//				processListIdentifiers();
//				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

	}

	/**
	 * The method simply starts at a given offset, identifies a given TLV, and then computes and returns the offset
	 * at which the next TLV is expected to start. It also throws exceptions when it encounters unexpected things
	 * @param buffer
	 * @param inOfs
	 * @param tag
	 * @param maxLen
	 * @return
	 */
	short checkTLV(byte[] buffer, short inOfs, byte tag, short maxLen)
	{
		if (buffer[inOfs++] != tag)
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		short len = buffer[inOfs++] ;
		if (len > maxLen)
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		return (short)(inOfs + len) ;
	}

	void processAddPassword(byte[] buf){
		// Checks the value of P1&P2
		if (Util.getShort(buf, ISO7816.OFFSET_P1)!=0) {
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
		// Checks the minimum length
		if ((short)(buf[ISO7816.OFFSET_LC]&0xFF) < 3) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		// Receives data and checks its length
		// 0xFF can be removed as a short otimization as it cannot go above 3, but
		//! This stupid trick only works because there is a minimum length check, which also
		// happens to refuse all negative lengths (since 0x80, whose signed value is -128,
		// happens to be smaller than 3). If you ever do this on a real application,
		// don't forget to include a nice comment, or it will look like a big bad bug.
		if (APDU.getCurrentAPDU().setIncomingAndReceive() != (short)(buf[ISO7816.OFFSET_LC]&0xFF)) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		// Checks the identifier
		short ofsId = ISO7816.OFFSET_CDATA ;
		//The first check verifies that we have a correct identifier TLV, and it returns the offset to the next entry, the user name.
		short ofsUserName = checkTLV(buf, ofsId, TAG_IDENTIFIER, PasswordEntry.SIZE_ID) ;
		//The second check verifies that there is enough room for the user name in the incoming data.
		if (buf[ISO7816.OFFSET_LC] < (short)(ofsUserName - 3)) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}

		// Checks the user name
		short ofsPassword = checkTLV(buf, ofsUserName, TAG_USERNAME, PasswordEntry.SIZE_USERNAME) ;
		if (buf[ISO7816.OFFSET_LC] < (short)(ofsPassword - 3)) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		// Checks the password
		if (checkTLV(buf, ofsPassword, TAG_PASSWORD, PasswordEntry.SIZE_PASSWORD) != (short)(ISO7816.OFFSET_CDATA + (short)(buf[ISO7816.OFFSET_LC]&0xFF))) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}

		// Search the identifier in the current base to ensure that it not already exists
		if (PasswordEntry.search(buf, (short)(ofsId+2), buf[(short)(ofsId+1)]) != null) {
			ISOException.throwIt(SW_DUPLICATE_IDENTIFIER);
		}

		// Allocates and initializes a password entry
		JCSystem.beginTransaction();
		PasswordEntry pe = PasswordEntry.getInstance();
		pe.setId(buf, (short)(ofsId+2), buf[(short)(ofsId+1)]);
		pe.setUserName(buf, (short)(ofsUserName+2), buf[(short)(ofsUserName+1)]);
		pe.setPassword(buf, (short)(ofsPassword+2), buf[(short)(ofsPassword+1)]);
		JCSystem.commitTransaction();
	}

	void processRetrivePasswordEntry(byte[] buff){
		// INITIAL CHECKS

		// Checks the value of P1&P2
		if (Util.getShort(buff, ISO7816.OFFSET_P1)!=0)
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		// Checks the minimum length
		if ((short)(buff[ISO7816.OFFSET_LC]&0xFF) < 3)
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		// Receives data and checks its length
		if (APDU.getCurrentAPDU().setIncomingAndReceive() != (short)(buff[ISO7816.OFFSET_LC]&0xFF))
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		// INTERPRETS AND CHECKS THE DATA

		// Checks the identifier
		if (checkTLV(buff, ISO7816.OFFSET_CDATA, TAG_IDENTIFIER, PasswordEntry.SIZE_ID) != (short)(ISO7816.OFFSET_CDATA + (short)(buff[ISO7816.OFFSET_LC]&0xFF)))
			ISOException.throwIt(ISO7816.SW_DATA_INVALID) ;

		// Search the identifier in the current base
		PasswordEntry pe = PasswordEntry.search(buff, (short)(ISO7816.OFFSET_CDATA+2), buff[ISO7816.OFFSET_CDATA+1]) ;
		if (pe==null)
			ISOException.throwIt(SW_IDENTIFIER_NOT_FOUND) ;

		// Builds the result, starting with the user name
		short outOfs = 0 ;
		buff[outOfs++] = TAG_USERNAME ;
		byte len = pe.getUserName(buff,(short)(outOfs+1)) ;
		buff[outOfs++] = len ;
		outOfs += len ;

		// Builds the result, continuing with the password
		buff[outOfs++] = TAG_PASSWORD ;
		len = pe.getPassword(buff,(short)(outOfs+1)) ;
		buff[outOfs++] = len ;
		outOfs += len ;

		// Sends the result
		APDU.getCurrentAPDU().setOutgoingAndSend((short)0, outOfs) ;
	}

	void processListIdentifiers(byte[] buf){
		// Checks P1 and initializes the "current" value
		if (buf[ISO7816.OFFSET_P1]==0)
			current = PasswordEntry.getFirst() ;
		else if ( (buf[ISO7816.OFFSET_P1]!=1) || (current==null) )
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);

		// Builds the response
		short offset = 0 ;
//		while(current != null) {
//			// Checks that the identifier record fits in the APDU
//			// WARNING: assumes a 256-byte APDU buffer
//			byte len = current.getIdLength();
//			if ((short) ((short) (offset + len) + 2) > 255)
//				break;
//		}
	}
}
