package com.mmd.dieta;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.util.Xml;
import android.widget.*;
import org.xmlpull.v1.XmlPullParser;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

final class DietWorkbook {
    static final int IMPORT=6031,EXPORT=6032;
    static final String[] HEADER={"dia","refeicao","horario","alimento","gramas","kcal_100g","proteina_100g","carboidrato_100g","gordura_100g"};
    final MainActivity a;
    int exportStart=1,exportCount=15,importStart=1;
    DietWorkbook(MainActivity a){this.a=a;}
    void buttons(LinearLayout c,int day){
        NutritionUI ui=new NutritionUI(a);
        ui.line(c,"Excel (.xlsx): dias relativos 1–7 ou 1–15; a partir do dia selecionado. Refeições confirmadas são preservadas.");
        ui.button(c,"Importar cardápio Excel",()->{importStart=day;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").addCategory(Intent.CATEGORY_OPENABLE);a.startActivityForResult(i,IMPORT);});
        ui.button(c,"Exportar cardápio Excel",()->new AlertDialog.Builder(a).setTitle("Quantos dias deseja exportar?").setItems(new String[]{"7 dias","15 dias"},(d,w)->{exportStart=day;exportCount=Math.min(w==0?7:15,PlanData.days(a)-day+1);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Cardapio_"+exportCount+"d.xlsx");a.startActivityForResult(i,EXPORT);}).setNegativeButton("Cancelar",null).show());
    }
    void result(int request,int result,Intent data){
        if((request!=IMPORT&&request!=EXPORT)||result!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();int start=request==IMPORT?importStart:exportStart,count=exportCount;
        // Parse on a background thread; only validated data can reach the save confirmation.
        new Thread(()->{try{if(request==IMPORT){Map<Integer,JSONArray> entries=read(uri);a.runOnUiThread(()->preview(entries,start));}else{write(uri,start,count);a.runOnUiThread(()->toast("Excel exportado."));}}catch(Exception e){a.runOnUiThread(()->toast("Planilha não processada: "+e.getMessage()));}}).start();
    }
    void toast(String s){Toast.makeText(a,s,Toast.LENGTH_LONG).show();}
    static String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    static void put(ZipOutputStream z,String name,String data)throws IOException{z.putNextEntry(new ZipEntry(name));z.write(data.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    void write(Uri uri,int start,int count)throws Exception{
        StringBuilder sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        String[] exportHeader=Arrays.copyOf(HEADER,13);System.arraycopy(new String[]{"kcal_porcao","proteina_g","carboidrato_g","gordura_g"},0,exportHeader,9,4);row(sheet,1,exportHeader);int rn=2;
        for(int d=start;d<start+count;d++)for(int m=0;m<4;m++){JSONArray list=NutritionData.effectiveMeal(a,d,m);if(list.length()==0)throw new Exception("Ative o ciclo Natural 60D ou personalize todas as refeições antes de exportar");for(int j=0;j<list.length();j++){JSONObject o=list.getJSONObject(j);row(sheet,rn++,new String[]{String.valueOf(d-start+1),String.valueOf(m+1),PlanData.mealTime(a,m),o.getString("name"),o.getString("g"),o.getString("k"),o.getString("p"),o.getString("c"),o.getString("f"),portion(o,"k"),portion(o,"p"),portion(o,"c"),portion(o,"f")});}}
        sheet.append("</sheetData></worksheet>");
        try(OutputStream os=a.getContentResolver().openOutputStream(uri,"wt");ZipOutputStream z=new ZipOutputStream(os)){
            put(z,"[Content_Types].xml","<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            put(z,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            put(z,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Cardapio\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(z,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            put(z,"xl/worksheets/sheet1.xml",sheet.toString());
        }
    }
    static String portion(JSONObject o,String key)throws JSONException{return String.format(Locale.US,"%.2f",o.getDouble("g")*o.getDouble(key)/100.0);}
    static void row(StringBuilder s,int n,String[] values){s.append("<row r=\"").append(n).append("\">");for(int i=0;i<values.length;i++){String ref=""+(char)('A'+i)+n;s.append("<c r=\"").append(ref).append("\"");if(n>1&&(i==0||i==1||i>=4))s.append("><v>").append(xml(values[i])).append("</v></c>");else s.append(" t=\"inlineStr\"><is><t>").append(xml(values[i])).append("</t></is></c>");}s.append("</row>");}
    static XmlPullParser parser(byte[] b)throws Exception{XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);p.setInput(new ByteArrayInputStream(b),"UTF-8");return p;}
    static List<String> strings(byte[] b)throws Exception{List<String> values=new ArrayList<>();if(b==null)return values;XmlPullParser p=parser(b);StringBuilder s=null;for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next()){if(e==2&&p.getName().equals("si"))s=new StringBuilder();else if(e==2&&p.getName().equals("t")&&s!=null)s.append(p.nextText());else if(e==3&&p.getName().equals("si")){values.add(s.toString());s=null;}}return values;}
    static int col(String ref){int n=0;for(int i=0;i<ref.length()&&Character.isLetter(ref.charAt(i));i++)n=n*26+Character.toUpperCase(ref.charAt(i))-'A'+1;return n-1;}
    static List<String[]> rows(byte[] b,List<String> shared)throws Exception{
        XmlPullParser p=parser(b);List<String[]> result=new ArrayList<>();String[] row=null;int column=-1;String type="",value="";boolean formula=false;
        for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next()){
            if(e==2){String n=p.getName();if(n.equals("row")){row=new String[9];Arrays.fill(row,"");}
                else if(n.equals("c")){String r=p.getAttributeValue(null,"r");column=r==null?-1:col(r);type=p.getAttributeValue(null,"t");value="";formula=false;}
                else if(n.equals("f")){formula=true;}
                else if(n.equals("v")||n.equals("t")){if(column>=0&&column<9)value+=p.nextText();}
            }else if(e==3){String n=p.getName();if(n.equals("c")&&row!=null&&column>=0&&column<9){if(formula)throw new Exception("Use valores, não fórmulas, nas colunas A–I");row[column]="s".equals(type)?shared.get(Integer.parseInt(value)):value;column=-1;}else if(n.equals("row")&&row!=null){boolean nonempty=false;for(String v:row)nonempty|=!v.trim().isEmpty();if(nonempty)result.add(row);if(result.size()>2001)throw new Exception("Muitas linhas");row=null;}}
        }return result;
    }
    Map<Integer,JSONArray> read(Uri uri)throws Exception{
        byte[] sheet=null,shared=null;int expanded=0;
        try(InputStream raw=a.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(raw)){ZipEntry e;byte[] buf=new byte[8192];int files=0;while((e=z.getNextEntry())!=null){if(++files>150)throw new Exception("Arquivo muito complexo");String name=e.getName();boolean keep=name.equals("xl/worksheets/sheet1.xml")||name.equals("xl/sharedStrings.xml");ByteArrayOutputStream out=new ByteArrayOutputStream();int n;while((n=z.read(buf))!=-1){expanded+=n;if(expanded>12000000)throw new Exception("Arquivo muito grande");if(keep)out.write(buf,0,n);}if(name.equals("xl/worksheets/sheet1.xml"))sheet=out.toByteArray();if(name.equals("xl/sharedStrings.xml"))shared=out.toByteArray();}}
        if(sheet==null)throw new Exception("Não encontrei a primeira aba do Excel");
        List<String[]> rows=rows(sheet,strings(shared));if(rows.size()<2||!Arrays.equals(rows.get(0),HEADER))throw new Exception("Cabeçalho incompatível; use a planilha modelo");
        Map<Integer,JSONArray> result=new TreeMap<>();int max=0;
        for(int i=1;i<rows.size();i++){String[] v=rows.get(i);int day=Integer.parseInt(v[0]),meal=Integer.parseInt(v[1]);if(day<1||day>15||meal<1||meal>4||!v[2].equals(new String[]{"07:15","11:30","15:00","19:00"}[meal-1])||v[3].trim().isEmpty()||v[3].length()>200)throw new Exception("Dia, refeição ou horário inválido na linha "+(i+1));
            double g=Double.parseDouble(v[4]),k=Double.parseDouble(v[5]),p=Double.parseDouble(v[6]),c=Double.parseDouble(v[7]),f=Double.parseDouble(v[8]);if(!Double.isFinite(g+k+p+c+f)||g<=0||g>2000||k<0||k>950||p<0||c<0||f<0||p+c+f>105)throw new Exception("Nutrientes inválidos na linha "+(i+1));int key=(day-1)*4+meal-1;JSONArray list=result.computeIfAbsent(key,x->new JSONArray());if(list.length()>=25)throw new Exception("Muitos alimentos por refeição");list.put(NutritionData.entry(v[3].trim(),g,k,p,c,f));max=Math.max(max,day);
        }
        if(max<1||max>15)throw new Exception("Use de 1 a 15 dias");for(int d=0;d<max;d++){double kcal=0;for(int m=0;m<4;m++){JSONArray list=result.get(d*4+m);if(list==null)throw new Exception("Faltam refeições no dia "+(d+1));kcal+=NutritionData.totals(list).kcal;}if(kcal<1500||kcal>6500)throw new Exception("Energia diária fora da faixa de importação no dia "+(d+1));}return result;
    }
    void preview(Map<Integer,JSONArray> entries,int start){
        int count=entries.size()/4;double total=0,protein=0;int skipped=0,applied=0;for(Map.Entry<Integer,JSONArray> e:entries.entrySet()){int d=start+e.getKey()/4,m=e.getKey()%4;if(d>PlanData.days(a)||PlanData.date(a,d).isBefore(LocalDate.now())||PlanData.mealDone(a,d,m)){skipped++;continue;}NutritionData.Totals t=NutritionData.totals(e.getValue());total+=t.kcal;protein+=t.p;applied++;}
        if(applied==0){toast("Nenhuma refeição disponível para importar.");return;}int skip=skipped;
        new AlertDialog.Builder(a).setTitle("Importar "+count+" dias de cardápio?").setMessage("A partir do dia "+start+". "+applied+" refeições serão substituídas; "+skip+" antigas, confirmadas ou fora do ciclo serão ignoradas.\nEnergia do arquivo (média por refeição aplicável): "+Math.round(total/applied)+" kcal • proteína "+Math.round(protein/applied)+" g.\nAs metas e os horários do protocolo permanecem.").setNegativeButton("Cancelar",null).setPositiveButton("Importar",(d,w)->{android.content.SharedPreferences.Editor ed=PlanData.prefs(a).edit();for(Map.Entry<Integer,JSONArray> e:entries.entrySet()){int day=start+e.getKey()/4,m=e.getKey()%4;if(day<=PlanData.days(a)&&!PlanData.date(a,day).isBefore(LocalDate.now())&&!PlanData.mealDone(a,day,m))ed.putString(NutritionData.mealKey(a,day,m),e.getValue().toString());}ed.apply();a.render();toast("Cardápio importado.");}).show();
    }
}