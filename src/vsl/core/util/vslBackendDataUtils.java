package vsl.core.util;

import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import vsl.core.data.*;
import vsl.core.types.*;

public class vslBackendDataUtils {


	public static String backendDataToString(int maxDataLen, vslBackendData data) {
		StringBuffer sb = new StringBuffer();
		// an array which we copy data into for display
		if (data instanceof vslVersionHeader) 
		{
			vslVersionHeader v = (vslVersionHeader) data;
			sb.append("[vslVersionHeader: id=[");
			sb.append(v.id.toString()).append("], prev=[ ");
			if (v.prevID != null)
			{
				for(vslID prev: v.prevID) 
				{
					sb.append(" {").append(prev.toString()).append("} ");
				}
			}
			sb.append("], createTime=[");
			if (v.createTime!= null) 
			{
				sb.append(v.createTime.toString());
			}
			sb.append("]");
		}
		if (data instanceof vslChunkHeader) 
		{
			vslChunkHeader v = (vslChunkHeader) data;
			sb.append("[vslChunkHeader: id=[");
			sb.append(v.id.toString()).append("], hash=[");
			if (v.hash!= null) 
			{
				sb.append(v.hash.toString());
			}
			sb.append("] createTime=[");
			if (v.createTime!= null) 
			{
				sb.append(v.createTime.toString());
			}
			sb.append("], extra=[");
			if (v.extra!= null) 
			{
				sb.append(v.extra.toString());
			}
			sb.append("], newInVersion=[").append(new Boolean(v.createdInVersion).toString()).append("]");
		}
		if (data instanceof vslChunkData) 
		{
			vslChunkData v = (vslChunkData) data;
			sb.append("[vslChunkData: hash=[");
			if (v.hash!= null) 
			{
				sb.append(v.hash.toString());
			}
			sb.append("] createTime=[");
			if (v.createTime!= null) 
			{
				sb.append(v.createTime.toString());
			}
			sb.append("], ");
			if (v.data!= null) 
			{
				//byte[] copy = new byte[maxDataLen];
				//System.arraycopy(v.data, 0, copy, 0, maxDataLen);
				//sb.append(new String(new Integer(copy.length).toString()));
				//sb.append(new String(new Integer(v.data.length).toString()));
				int showLen = Math.min(v.data.length, maxDataLen);
				sb.append("data[first ").append(new
					Integer(showLen).toString()).append(" of ").append(new
					Integer(v.data.length).toString()).append(" bytes]=[");
				sb.append(new String(v.data, 0, showLen));
			}
			else
			{
				sb.append("data[");
			}
			sb.append("], extra=[");
			if (v.extra!= null) 
			{
				sb.append(v.extra.toString());
			}
			sb.append("]");
		}
		if (data instanceof vslIndexCreateRecord) 
		{
			vslIndexCreateRecord v = (vslIndexCreateRecord) data;
			sb.append("[vslIndexCreateRecord: hash=[");
			if (v.hash!= null) 
			{
				sb.append(v.hash.toString());
			}
			sb.append("] createTime=[");
			if (v.createTime!= null) 
			{
				sb.append(v.createTime.toString());
			}
			sb.append("], elementKey=[");
			if (v.elKey!= null) 
			{
				sb.append(v.elKey.toString());
			}
			sb.append("], record Key=[");
			if (v.recKey!= null) 
			{
				sb.append(v.recKey.toString());
			}
			sb.append("], ");
			if (v.data!= null) 
			{
				byte[] vdata = v.data.toString().getBytes();
				//byte[] copy = new byte[maxDataLen];
				//System.arraycopy(v.data, 0, copy, 0, maxDataLen);
				//sb.append(new String(new Integer(copy.length).toString()));
				//sb.append(new String(new Integer(v.data.length).toString()));
				int showLen = Math.min(vdata.length, maxDataLen);
				sb.append("data[first ").append(new
					Integer(showLen).toString()).append(" of ").append(new
					Integer(vdata.length).toString()).append(" bytes]=[");
				sb.append(new String(vdata, 0, showLen));
			}
			else
			{
				sb.append("data[");
			}
			sb.append("]");
			//sb.append(" stringdata[").append(v.data.toString()).append("]");
		}
		if (data instanceof vslIndexUpdateRecord) 
		{
			vslIndexUpdateRecord v = (vslIndexUpdateRecord) data;
			sb.append("[vslIndexUpdateRecord: hash=[");
			if (v.hash!= null) 
			{
				sb.append(v.hash.toString());
			}
			sb.append("] createTime=[");
			if (v.createTime!= null) 
			{
				sb.append(v.createTime.toString());
			}
			sb.append("], elementKey=[");
			if (v.elKey!= null) 
			{
				sb.append(v.elKey.toString());
			}
			sb.append("], record Key=[");
			if (v.recKey!= null) 
			{
				sb.append(v.recKey.toString());
			}
			sb.append("], ");
			if (v.data!= null) 
			{
				byte[] vdata = v.data.toString().getBytes();
				//byte[] copy = new byte[maxDataLen];
				//System.arraycopy(v.data, 0, copy, 0, maxDataLen);
				//sb.append(new String(new Integer(copy.length).toString()));
				//sb.append(new String(new Integer(v.data.length).toString()));
				int showLen = Math.min(vdata.length, maxDataLen);
				sb.append("data[first ").append(new
					Integer(showLen).toString()).append(" of ").append(new
					Integer(vdata.length).toString()).append(" bytes]=[");
				sb.append(new String(vdata, 0, showLen));
			}
			else
			{
				sb.append("data[");
			}
			sb.append("], prevs=[");
			if (v.prev != null)
			{
				for (vslRecKey prev: v.prev) {
					sb.append(prev.toString()).append(", ");
				}
			}
			sb.append("]");
			//sb.append(" stringdata[").append(v.data.toString()).append("]");
		}
		return sb.toString();
	}


    public static byte[] serialize(Object obj) 
	{
		try {
      	  ByteArrayOutputStream b = new ByteArrayOutputStream();
		  ObjectOutputStream o = new ObjectOutputStream(b);
          o.writeObject(obj);
          return b.toByteArray();
		}
		catch (IOException e) {
		}
		return null;
    }



}
