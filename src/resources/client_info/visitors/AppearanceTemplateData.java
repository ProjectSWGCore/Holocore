package resources.client_info.visitors;

import java.util.ArrayList;
import java.util.List;

import resources.Point3D;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class AppearanceTemplateData extends ClientData {
	
	private List<Point3D> radarPoints = new ArrayList<>();
	
	@Override
	public void readIff(SWGFile iff) {
		readForms(iff, 0);
	}
	
	private void readForms(SWGFile iff, int depth) {
		IffNode form = null;
		while ((form = iff.enterNextForm()) != null) {
			switch (form.getTag()) {
				case "RADR":
					loadRadar(iff);
					break;
				default:
					readForms(iff, depth+1);
//					System.err.println("Unknown APT form: " + form.getTag());
					break;
			}
			iff.exitForm();
		}
	}
	
	private void loadRadar(SWGFile iff) {
		IffNode node = iff.enterChunk("INFO");
		boolean hasRadar = node.readInt() != 0;
		if (hasRadar) {
			iff.enterForm("IDTL");
			iff.enterForm("0000");
			IffNode chunk = iff.enterChunk("VERT");
			List<Point3D> points = new ArrayList<>(chunk.remaining() / 12);
			while (chunk.remaining() >= 12) {
				points.add(chunk.readVector());
			}
			chunk = iff.enterChunk("INDX");
			while (chunk.remaining() >= 4) {
				radarPoints.add(new Point3D(points.get(chunk.readInt())));
			}
			iff.exitForm();
			iff.exitForm();
		}
	}
	
	public List<Point3D> getRadarPoints() {
		return radarPoints;
	}
	
}
