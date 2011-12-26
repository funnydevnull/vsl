package vsl.core.util;



import vsl.core.data.*;
import vsl.core.types.*;

public class vslBackendDataUtils {


	public static String backendDataToString(vslBackendData data) {
		StringBuffer sb = new StringBuffer();
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
			sb.append("]");
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
			sb.append("], data=[");
			if (v.data!= null) 
			{
				sb.append(new String(v.data));
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
