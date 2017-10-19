/*
 * TapBodyTest.java
 *
 *  created: 10.10.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.data2tap;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Martin Pokorný
 * @version 0.1
 */
public class TapBodyTest {
    
    @Test
    public void testTapBody() {
        TapBody tapBody = new TapBody(8);
        tapBody.append((byte) 0xFE);
        tapBody.append((byte) 0xEF);
        tapBody.append((byte) 0x13);
        tapBody.append((byte) 0x67);
        tapBody.append((byte) 0x67);
        tapBody.append((byte) 0x67);
        tapBody.append((byte) 0x67);
        tapBody.append((byte) 0x67);
        tapBody.appendParityToLastByte();
        byte[] result = tapBody.getBytes();
                
        byte[] expected = new byte[]{
            (byte) 0x0A, (byte) 0x0,  // délka i paritou a flag - lsb,Msb  (= 8 + 2)
            (byte) 0xFF,    // Flag pro data
            (byte) 0xFE, (byte) 0xEF, (byte) 0x13, (byte) 0x67,
            (byte) 0x67, (byte) 0x67, (byte) 0x67, (byte) 0x67,
            (byte) 0x9A,    // parita
        };
        
        assertArrayEquals(expected, result);
    }

}   // TapBodyTest.java