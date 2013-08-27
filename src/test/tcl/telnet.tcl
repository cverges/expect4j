package require java
java::load expect4j.ExpectEmulation
package require expect

#Spawn a telnet session to the telnet server running on the target device
spawn telnet 192.168.0.1
 
expect "login: "
send "root\r"
expect "Password:"
send "supersecret\r"
 
#Put a expression which you expect at the command prompt
# For example:
# A prompt like "[anil@Testers anil]$" requires
# expect "*anil]$ "
expect "root@router:~# "

