/*
 * Copyright 2007 Justin Ryan
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package expect4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author justin
 */
public abstract class NioConsumer extends ConsumerImpl {
    
    /** Creates a new instance of NioConsumer */
    public NioConsumer(IOPair pair) throws Exception {
        super(pair);
        
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(host, port);
        socketChannel.connect(isa);
        selector = Selector.open();
        int interest = 0;
        
        if(socketChannel.isConnected())interest = SelectionKey.OP_READ;
        else if(socketChannel.isConnectionPending())interest = SelectionKey.OP_CONNECT;
        
        socketChannel.register(selector, interest);
    }
    
    abstract public void run();
    abstract public void waitForBuffer(long timeoutMilli);
    abstract public String pause();
    abstract public void resume(int offset);
    
    
    private String host = "localhost";
    private int port = 5001;
    private SocketChannel socketChannel;
    private Selector selector;
/*    
    public void run() {
        try {
            
            while(true) {
                int nn = selector.select();
                System.out.println("nn="+nn);
                Set keys = selector.selectedKeys();
                for(Iterator i = keys.iterator(); i.hasNext();) {
                    SelectionKey key = (SelectionKey) i.next();
                    i.remove();
                    if (key.isConnectable()) {
                        SocketChannel keyChannel = (SocketChannel) key.channel();
                        System.out.println("Connected "+keyChannel.finishConnect());
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        SocketChannel keyChannel = (SocketChannel) key.channel();
                        String m = read(keyChannel);
                        display(m);
                    }
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public void send(final String m) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    write(m, socketChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        });
        t.start();
        
    }
    public static String read(SocketChannel channel) throws IOException {
        
        log("*** start READ");
        int n;
        
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        while((n = channel.read(buffer)) > 0) {
            System.out.println("     adding "+n+" bytes");
        }
        
        
        log("  BUFFER REMPLI : "+buffer);
        
        buffer.flip();
        
        CharBuffer cb = dec.decode(buffer);
        log("  CHARBUFFER : "+cb);
        
        
        String m = cb.toString();
        log("  MESSAGE : "+m);
        log("*** end READ");
        //buffer.clear();
        return m;
    }
    public static void write(String m, SocketChannel channel) throws IOException {
        
        log("xxx start WRITE");
        
        CharBuffer cb = CharBuffer.wrap(m);
        log("  CHARBUFFER : "+cb);
        
        ByteBuffer  buffer = enc.encode(cb);
        log("  BUFFER ALLOUE REMPLI : "+buffer);
        
        int n;
        while(buffer.hasRemaining()) {
            n = channel.write(buffer);
        }
        System.out.println("  REMAINING : "+buffer.hasRemaining());
        log("xxx end WRITE");
    }
 */
}
