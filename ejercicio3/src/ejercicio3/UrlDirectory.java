package ejercicio3;

import java.util.List;
import java.util.ArrayList;

public class UrlDirectory {

		protected List<String> urlList;
		private String subject;
		
		public UrlDirectory() {
			subject = "variado";
			urlList = new ArrayList<String>();
		}
		
		public String getSubject() {
			return subject;
		}
		
		public void setSubject(String subject) {
			this.subject = subject;
		}
		
		public void addUrl (String url) {
			urlList.add(url);
		}
		
		public void removeUrl(String url) {
			urlList.remove(url);
		}
		
		public void removeUrl (int idx) {
			urlList.remove(idx);
		}
		
		public int getDirectorySize() {
			return urlList.size();
		}
		
		public String getUrl (int idx) {
			return urlList.get(idx);
		}
}
