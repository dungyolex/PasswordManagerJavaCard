package applet;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class PasswordEntry {
    public static final byte SIZE_USERNAME = (byte)24;
    public static final byte SIZE_PASSWORD = (byte)16;
    public static final byte SIZE_ID = (byte)16;

    private byte[] id;
    private byte[] userName;
    private byte[] password;
    private byte idLength ;
    private byte userNameLength ;
    private byte passwordLength ;

    private PasswordEntry next ;
    private static PasswordEntry first ;
    private static PasswordEntry deleted ;

    /**
     * In this method, we insert an element in a list. This is done in two steps. First, the successor of
     * the new element is set to the lists first element; then, the new element is inserted as the first
     * element. In that case, the atomicity provided by Java Card on all updates is sufficient to guarantee
     * that the list will always remain consistent. This is possible because the operation that actually
     * inserts the element in the list is the last one, and it is performed in a single write operation.
     */
    private PasswordEntry()
    {
        // Allocates all fields
        id = new byte[SIZE_ID] ;
        userName = new byte[SIZE_USERNAME] ;
        password = new byte[SIZE_PASSWORD] ;
        // The new element is inserted in front of the list
        next = first ;
        first = this ;
    }

    static PasswordEntry getInstance()
    {
        if (deleted == null)
        {
            // There is no element to recycle
            return new PasswordEntry() ;
        }
        else
        {
            // Recycle the first available element
            PasswordEntry instance = deleted ;
            JCSystem.beginTransaction() ;
            deleted = instance.next ;
            first = instance ;
            instance.next = first ;
            JCSystem.commitTransaction() ;
            return instance ;
        }
    }

    void setId(byte[] newId, short ofs, short idlength){
        if(idLength!=SIZE_ID){
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopy(newId, (short)ofs, this.id, (short) 0, idlength);
    }

    void setUserName(byte[] newUserName, short ofs, short newUserNameLength){
        if(userNameLength!=SIZE_USERNAME){
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopy(newUserName, (short)ofs, this.userName, (short) 0, newUserNameLength);
    }

    void setPassword(byte[] newPassword, short ofs, short newPasswordLength){
        if(passwordLength!=SIZE_PASSWORD){
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopy(newPassword, (short) ofs, this.password, (short) 0, newPasswordLength);
    }

    public byte getId(byte[] buf, short ofs) {
        Util.arrayCopy(id, (short)0, buf, ofs, idLength) ;
        return idLength ;
    }

    public byte getUserName(byte[] buf, short ofs) {
        Util.arrayCopy(userName, (short)0, buf, ofs, userNameLength) ;
        return userNameLength ;
    }

    public byte getPassword(byte[] buf, short ofs) {
        Util.arrayCopy(password, (short)0, buf, ofs, passwordLength) ;
        return passwordLength ;
    }

    public static PasswordEntry getFirst(){
        return first;
    }

    static PasswordEntry search(byte[] buf, short ofs, byte len) {
        for(PasswordEntry pe = first ; pe != null ; pe = pe.next)
        {
            if (pe.idLength != len) continue ;
            if (Util.arrayCompare(pe.id, (short)0, buf, ofs, len)==0)
                return pe ;
        }
        return null ;
    }

    private void remove()
    {
        if (first==this)
            first = next ;
        else
        {
            for(PasswordEntry pe = first ; pe != null ; pe = pe.next)
                if (pe.next == this)
                    pe.next = next ;
        }
    }

    private void recycle()
    {
        next = deleted ;
        deleted = this ;
    }

    public static void delete(byte[] buf, short ofs, byte len)
    {
        PasswordEntry pe = search(buf, ofs, len) ;
        if (pe != null)
        {
            JCSystem.beginTransaction() ;
            pe.remove() ;
            pe.recycle() ;
            JCSystem.commitTransaction() ;
        }
    }
}
