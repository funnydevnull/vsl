package vsl.core.util;



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
		return sb.toString();
	}


}
