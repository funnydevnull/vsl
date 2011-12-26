package vsl.debug;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Got this from:
 *  http://herebebeasties.com/2007-02-08/javaionotserializableexception-in-your-httpsession/
 */

 
public class DebuggingObjectOutputStream extends ObjectOutputStream {
 
  private static final long serialVersionUID = 1L;
 
  private LinkedList stack = new LinkedList();
  private HashSet set = new HashSet();
 
  public DebuggingObjectOutputStream() throws IOException {}
 
  @Override
  protected final void writeObjectOverride(Object obj) throws IOException {
    // Check for circular reference.
    if (set.contains(obj)) {
      return;
    }
    if (stack.isEmpty()) {
      stack.add("Class " + obj.getClass().getName());
    }
	System.out.println("Serializing: " + obj.getClass().getName());
    set.add(obj);
    Field[] fields = obj.getClass().getFields();
	System.out.println("Object[ " + obj.getClass().getName() + "] has " + fields.length 
						+ " fields.");
    for (int i = 0; i < fields.length; i++) {
      StringBuffer buffer = new StringBuffer();
      Field f = fields[i];
      int m = f.getModifiers();
      if (fields[i].getType().isPrimitive() || Modifier.isTransient(m) ||
          Modifier.isStatic(m)) {
        continue;
      }
 
      if (Modifier.isPrivate(m)) {
        buffer.append("private ");
      }
      if (Modifier.isProtected(m)) {
        buffer.append("protected ");
      }
      if (Modifier.isPublic(m)) {
        buffer.append("public ");
      }
      if (Modifier.isFinal(m)) {
        buffer.append("final ");
      }
      if (Modifier.isVolatile(m)) {
        buffer.append("volatile ");
      }
      buffer.append(f.getType().getName()).append("");
      buffer.append(" ").append(f.getName());
      stack.add(buffer.toString());
      if (Serializable.class.isAssignableFrom(fields[i].getType())) {
        try {
          writeObjectOverride(fields[i].get(obj));
        }
        catch (IllegalAccessException e) {
          throw new RuntimeException(
              getPrettyPrintedStack(fields[i].getType().getName()), e);
        }
      }
 
      else {
        throw new RuntimeException(
            getPrettyPrintedStack(fields[i].getType().getName()).toString(),
            new NotSerializableException(fields[i].getType().getName())
        );
      }
      stack.removeLast();
    }
    if (stack.size() == 1) {
      set.clear();
      stack.removeLast();
    }
  }
 
  private String getPrettyPrintedStack(String type) {
    set.clear();
    StringBuffer result = new StringBuffer();
    StringBuffer spaces = new StringBuffer();
    result.append("Unable to serialize class: ");
    result.append(type);
    result.append("\nField hierarchy is:");
    while (!stack.isEmpty()) {
      spaces.append("  ");
      result.append("\n").append(spaces).append(stack.removeFirst());
    }
    result.append(" <----- field that is not serializable");
    return result.toString();
  }
}
