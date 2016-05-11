
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.HtmlPage;

//https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=AAPL&type=10-Q&dateb=&owner=exclude&count=100
//涓ゅ眰閾炬帴锛屼繚瀛樺埌鏂囦欢涓�
//涓嬭浇缃戦〉
public class getDocUrls {

	/**
	 * @param args
	 */
	public static int MAX_THREAD=27;
	
	public static String[][] Data={{"1","AAPL","0"}};
	public static String FILE_PATH="d:\\Coorps";
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		
		//BufferedReader br=new BufferedReader("d://corps.txt");
		Data=getURLs("d://Coorps/10-Qurls.txt");
		WriteToExcel();
		

	}
	public static String[][] getURLs(String path) throws Exception
	{
		int count=0;
		File f = new File(path);
		InputStream input = new FileInputStream(f);
		BufferedReader b = new BufferedReader(new InputStreamReader(input));
		String value = b.readLine();
		if(value != null)while(value !=null){
		 count++;
		 value = b.readLine();
		}b.close();
		input.close();
		System.out.println(count);
		String[][] result=new String[count][4];
		for(int i=0;i<count;i++)
		{
			result[i][0]=(i+1)+"";
			result[i][3]="0";
		}
		BufferedReader br = new BufferedReader(new FileReader(path));
		int index=0;
		while(br.ready())
		{
			String line=br.readLine();
			if(line.split("####").length==2)
			{
				result[index][1]=line.split("####")[1];
				result[index][2]=line.split("####")[0];
			}
			
			//System.out.println(result[index][1]);
			index++;
		}
		br.close();
		return result;
	}
	public static WritableSheet sheet=null;
	public static void WriteToExcel()
	{
		File outputFile = new File(FILE_PATH + "\\" + "gen_data.xls");
		OutputStream os = null;
		WritableWorkbook book=null;
		try {
			if (!outputFile.exists()) {
				outputFile.createNewFile();
			}
			os = new FileOutputStream(outputFile);
		Label label;
		book = Workbook.createWorkbook(os);
		sheet = book.createSheet("sheet1", 0);
		label = new Label(0, 0, "corps");
		sheet.addCell(label);
		/*
		HashMap<String,String> DataMap=KclGetDetails(Data[0]);
		putIntoWorkbook(DataMap,Integer.parseInt(Data[0][0]));
		*/
		
		ExecutorService pool = Executors.newCachedThreadPool();
		for(int tIndex=0;tIndex<MAX_THREAD;tIndex++)
		{
			pool.execute(
			new Runnable(){
				public void run()
				{
					while(true)
					{
						String[] data= getUnhandledURL();
						if(data!=null)
						{
							//System.out.println(data.length);
							//System.out.println(data[0]+data[1]+data[2]);
							HashMap<String,String> DataMap=GetDetails(data);
							putIntoWorkbook(DataMap,Integer.parseInt(data[0]));
							System.out.println(data[0]+" done.");
						}
						else{
							//System.out.println("No Unhandled");
							break;
						}
					}
					
					
				}
			});
		}
		pool.shutdown();
		pool.awaitTermination(600, TimeUnit.SECONDS);
		
		
       
		}
		catch(Exception ee)
		{
			ee.printStackTrace();
		}finally{
			
			try {
				book.write();
				book.close();
				os.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (WriteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}}
	}
	
	public static synchronized String[] getUnhandledURL()
	{
		for(int i=0;i<Data.length;i++)
		{
			if(Data[i][Data[0].length-1].equals("0"))
			{
				Data[i][Data[0].length-1]="1";
				return Data[i];
			}
		}
		return null;
	}
	
	public static synchronized void putIntoWorkbook(HashMap<String,String> data,int index)
	{
		String[] Keys={"corps"};
		for(int j=0;j<1;j++)
		{
			//label = new Label(j, i, data.get(Keys[j]));
		    Label label = new Label(j, index, data.get(Keys[j]));
		    try{
		    	sheet.addCell(label);
		    }
		    catch(Exception ee)
		    {
		    	ee.printStackTrace();
		    }
			
		}
	}
	
	

	public static HashMap<String,String> GetDetails(String[] url)
	{
		
		while(true)
		{
			try{
				HashMap<String,String> result=new HashMap<String,String>();
				RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();  
				CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();  
				
				HttpGet httpGet = new HttpGet(url[1]); 
				HttpResponse response = httpclient.execute(httpGet);  
				HttpEntity entity = response.getEntity();
				
				String htmls=null;
				if (entity != null) { 
				    htmls=EntityUtils.toString(entity).replace("\t", " ");//.replace("<meta", "<form");
				    //System.out.println(htmls);
				    
				     
				}
				//System.out.println("Got reply!");
				//htmls=HTMLFilter(htmls);
				
				Parser parser=null;
			    HtmlPage page=new HtmlPage(parser); 
			    parser=Parser.createParser(htmls, "utf-8");
		        AndFilter FFilter=new AndFilter(new TagNameFilter("table"),
		        		new HasAttributeFilter("class","tableFile"));
		        NodeList nodes6 = parser.extractAllNodesThatMatch(FFilter);
		        
		        if(nodes6.size()>0)
		        {
		        	
		        	for(int i=0;i<nodes6.size();i++)
		        	{
		        		if(nodes6.elementAt(i).toHtml().contains("summary=\"Document Format Files\""))
		        		{
		        			//System.out.println("in");
		        			parser=Parser.createParser(nodes6.elementAt(i).toHtml(), "utf-8");
					   	    AndFilter ProfessionNameFilter=new AndFilter(new TagNameFilter("a"),
					                   new HasAttributeFilter("href"));
					   	    NodeList nodes4=parser.extractAllNodesThatMatch(ProfessionNameFilter);
					   	    for(int j=0;j<nodes4.size();j++)
					   	    {
					   	    	LinkTag link=(LinkTag)nodes4.elementAt(j);
					   	    	if(!link.getAttribute("href").equals(""))
					   	    	{
					   	    		//write2File("Corps",url[1]+"####https://www.sec.gov"+link.getAttribute("href"));
					   	    		//System.out.println(html2Str(link.toHtml())+":"+"https://www.sec.gov"+link.getAttribute("href"));
					   	    		httpGet = new HttpGet("https://www.sec.gov"+link.getAttribute("href")); 
									HttpResponse response2 = httpclient.execute(httpGet);  
									HttpEntity entity2 = response2.getEntity();
									
									String htmls2=null;
									if (entity2 != null) { 
									    htmls2=EntityUtils.toString(entity2).replace("\t", " ");//.replace("<meta", "<form");
									    //System.out.println("d://lj/Corps/"+url[2]+"/"+html2Str(link.toHtml()));
									    if(html2Str(link.toHtml()).equals(""))
									    {
									    	write2File("d://Coorps/"+url[2]+"/",url[0]+".html",htmls2);
									    }
									    else
									    write2File("d://Coorps/"+url[2]+"/",html2Str(link.toHtml()),htmls2);
									     
									}
					   	    		
					   	    		break;
					   	    	}
					   	    	
					   	    }
		        		}
		        	}
		    		
		    		//System.out.println(title);
		    		
		        }
			    
		        
			    
			    
			    
			    
				httpclient.close();
		        return result;
			}
			catch(Exception ee)
			{
				System.out.println("Retrying..."+url[0]);
				ee.printStackTrace();
			}
		}
		
	}//...
	
	public static synchronized  void write2File(String path,String filename,String text) throws Exception
	{
		
		String Path=path;
		 File file = new File(Path);
		   if(!file.exists()){
		    file.mkdirs();
		   }
		FileOutputStream fo=new FileOutputStream(path+filename,false);
		
		//fo.write(text.getBytes("GBK"));
		
		fo.write(text.getBytes());
		fo.close();
	}
	
	public static String html2Str(String html) { 
		return html.replaceAll("<[^>]+>", "");
	}
	
	public static String toUpperCaseFirstOne(String s)
    {
        if(Character.isUpperCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
    }
	
		public static String HTMLFilter(String input) {
		    if (input == null) {
		        input = "";
		        return input;
		    }
		    input = input.trim().replaceAll("&amp;", "&");
		    input = input.trim().replaceAll("&lt;", "<");
		    input = input.trim().replaceAll("&gt;", ">");
		    input = input.trim().replaceAll("    ", " ");
		    input = input.trim().replaceAll("<br>", "\n");
		    input = input.trim().replaceAll("&nbsp;", "  ");
		    input = input.trim().replaceAll("&quot;", "\"");
		    input = input.trim().replaceAll("&#39;", "'");
		    input = input.trim().replaceAll("&#92;", "\\\\");
		    input = input.trim().replaceAll("&#...;", "");
		    input = input.trim().replaceAll("&#....;", "");
		    return input;
		}

}
