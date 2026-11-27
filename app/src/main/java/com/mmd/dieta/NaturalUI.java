package com.mmd.dieta;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.LocalDate;
import java.util.Locale;

final class NaturalUI {
    final MainActivity a; final NutritionUI ui; final SharedPreferences p;
    final Handler handler=new Handler(Looper.getMainLooper());
    AlertDialog cardioDialog; Runnable tick; int photoWeek=1;
    static final int PHOTO_REQUEST=6021;
    NaturalUI(MainActivity a){this.a=a;ui=new NutritionUI(a);p=PlanData.prefs(a);}
    String key(int d,String s){return PlanData.key(a,d,"natural_"+s);}
    int week(int d){return (d-1)/7+1;}
    String weekKey(int w,String s){return "n60_week_"+PlanData.start(a)+"_"+w+"_"+s;}
    int dp(int n){return ui.dp(n);}
    void home(LinearLayout c,int d){
        if(!PlanData.natural(a)){ui.line(c,"Novo ciclo de 60 dias • cardápio sem azeite, refeições nos horários dos treinos, recuperação e avaliações semanais.");ui.button(c,"Iniciar projeto de 60 dias",()->a.activateNatural());return;}
        int today=PlanData.today(a);boolean ended=LocalDate.now().isAfter(PlanData.date(a,60));
        c.addView(ui.text(ended?"Ciclo concluído":"DIA "+today+" / 60",28,NutritionUI.CYAN,true));
        ui.bar(c,"Progresso do calendário",today,60,NutritionUI.CYAN,"dias");
        ui.line(c,"Semana "+week(d)+" • "+Math.max(0,60-today)+" dias após o dia atual • selecionado: dia "+d);
        ui.line(c,"06h caminhada opcional • 13h força • 17h30 corrida nos dias previstos. Preserve os dias de recuperação.");
        ui.button(c,"Check-in: sono, disposição e recuperação",()->checkin(d));
        ui.button(c,"Suplementação e tratamento",()->supplements(d));
        ui.button(c,"Refeição livre da semana",()->freeMeal(d));
        ui.button(c,"Jejum opcional • registro",()->fast(d));
        ui.button(c,"Avaliação e revisão semanal",()->weekly(d));
        String sleep=p.getString(key(d,"sleep"),"");if(!sleep.isEmpty())ui.line(c,"Sono registrado: "+sleep+" h • disposição "+p.getInt(key(d,"energy"),3)+"/5");
        ui.line(c,"Sem cortes calóricos automáticos, sem jejum compensatório. Objetivo: saúde, força e bem-estar.");
    }
    LinearLayout form(){LinearLayout c=ui.vertical();c.setPadding(dp(16),dp(6),dp(16),dp(12));return c;}
    ScrollView scroll(LinearLayout c){ScrollView s=new ScrollView(a);s.addView(c);return s;}
    void toast(String s){Toast.makeText(a,s,Toast.LENGTH_LONG).show();}
    void checkin(int day){
        LinearLayout c=form();EditText sleep=ui.field(c,"Horas dormidas",p.getString(key(day,"sleep"),""),true);
        String[] rates={"1 • Muito baixa","2 • Baixa","3 • Regular","4 • Boa","5 • Excelente"};
        ui.line(c,"Disposição");Spinner energy=ui.spinner(c,rates,p.getInt(key(day,"energy"),3)-1);
        ui.line(c,"Recuperação muscular");Spinner recovery=ui.spinner(c,rates,p.getInt(key(day,"recovery"),3)-1);
        ui.line(c,"Força percebida");Spinner strength=ui.spinner(c,rates,p.getInt(key(day,"strength"),3)-1);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Check-in diário").setView(scroll(c)).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();d.show();
        d.getButton(-1).setOnClickListener(v->{try{double h=ui.val(sleep);if(!Double.isFinite(h)||h<0||h>24)throw new Exception();p.edit().putString(key(day,"sleep"),String.valueOf(h)).putInt(key(day,"energy"),energy.getSelectedItemPosition()+1).putInt(key(day,"recovery"),recovery.getSelectedItemPosition()+1).putInt(key(day,"strength"),strength.getSelectedItemPosition()+1).apply();d.dismiss();a.render();if(h<7||energy.getSelectedItemPosition()<2||recovery.getSelectedItemPosition()<2)toast("Considere reduzir a carga e descansar. Se persistir, procure avaliação profissional.");}catch(Exception e){sleep.setError("Informe de 0 a 24 horas");}});
    }
    void supplements(int day){
        LinearLayout c=form();ui.line(c,"Creatina monohidratada: 3–5 g/dia, se adequada para você. Whey apenas se necessário para completar proteína. Não use estimulantes hormonais sem orientação.");
        String[] names={"Creatina","Whey (se necessário)","Tratamento prescrito (conforme orientação médica)","Outro suplemento orientado"};
        for(int i=0;i<names.length;i++){final int n=i;CheckBox cb=new CheckBox(a);cb.setText(names[i]);cb.setChecked(p.getBoolean(key(day,"supp"+i),false));c.addView(cb);cb.setOnCheckedChangeListener((b,checked)->p.edit().putBoolean(key(day,"supp"+n),checked).apply());}
        ui.line(c,"O aplicativo não recomenda dose de medicamentos nem modifica seu tratamento. Confirme somente o que realmente tomou.");
        new AlertDialog.Builder(a).setTitle("Controle de suplementação").setView(scroll(c)).setPositiveButton("Fechar",null).show();
    }
    void freeMeal(int day){
        int w=week(day);LinearLayout c=form();ui.line(c,"Uma refeição livre por semana, não um dia inteiro. Mantenha as demais refeições normalmente; não compense com restrição, jejum ou exercício.");
        int suggested=Math.min(60,w*7);ui.line(c,"Sugestão: dia "+suggested+" • "+PlanData.date(a,suggested)+". Escolha outro dia da mesma semana se preferir.");
        int used=p.getInt(weekKey(w,"free"),0);ui.line(c,used==0?"Ainda não registrada nesta semana.":"Registrada no dia "+used+".");
        ui.button(c,used==day?"Desfazer registro":"Registrar refeição livre no dia "+day,()->{if(used==day)p.edit().remove(weekKey(w,"free")).apply();else p.edit().putInt(weekKey(w,"free"),day).apply();toast("Registro atualizado.");a.render();});
        ui.line(c,"Para contabilizar energia e macros, edite a refeição correspondente na aba Dieta com os alimentos realmente consumidos. O selo não altera calorias.");
        new AlertDialog.Builder(a).setTitle("Refeição livre • semana "+w).setView(scroll(c)).setPositiveButton("Fechar",null).show();
    }
    void fast(int day){
        LinearLayout c=form();ui.line(c,"Jejum é opcional e não é necessário para emagrecer. Não use para compensar refeições. Priorize alimentação suficiente antes e depois dos treinos; interrompa se houver tontura ou mal-estar.");
        EditText h=ui.field(c,"Intervalo noturno realmente realizado (horas)",p.getString(key(day,"fast"),""),true);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Registro opcional de jejum").setView(c).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();d.show();d.getButton(-1).setOnClickListener(v->{try{double x=ui.val(h);if(!Double.isFinite(x)||x<0||x>24)throw new Exception();p.edit().putString(key(day,"fast"),String.valueOf(x)).apply();d.dismiss();a.render();}catch(Exception e){h.setError("Informe o intervalo realizado");}});
    }
    void cardio(LinearLayout c,int day){
        int mins=p.getInt(key(day,"cardio"),0);ui.line(c,"Caminhada leve • comece com 20–30 min e aumente se estiver bem recuperado. Jejum não é obrigatório.");
        ui.line(c,"Registrado neste dia: "+mins+" min • abdominal: "+(p.getBoolean(key(day,"abs"),false)?"registrado":"não registrado"));
        ui.button(c,"Cronômetro de caminhada",()->chooseCardio(day));
        ui.button(c,"Registrar abdominal / desfazer",()->{p.edit().putBoolean(key(day,"abs"),!p.getBoolean(key(day,"abs"),false)).apply();a.render();});
    }
    void chooseCardio(int day){String[] options={"20 minutos","30 minutos","40 minutos","45 minutos","60 minutos"};int[] times={20,30,40,45,60};new AlertDialog.Builder(a).setTitle("Duração • caminhada leve").setItems(options,(d,i)->timer(day,times[i])).setNegativeButton("Cancelar",null).show();}
    void timer(int day,int minutes){
        LinearLayout c=form();MotionView motion=new MotionView(a);c.addView(motion,new LinearLayout.LayoutParams(-1,dp(155)));motion.show("CAM • Caminhada",true);
        TextView clock=ui.text("",46,NutritionUI.CYAN,true);clock.setGravity(Gravity.CENTER);c.addView(clock);
        long[] remaining={minutes*60000L},end={SystemClock.elapsedRealtime()+remaining[0]};boolean[] running={true};
        Button pause=ui.button(c,"Pausar",()->{});pause.setOnClickListener(v->{if(running[0]){remaining[0]=Math.max(0,end[0]-SystemClock.elapsedRealtime());running[0]=false;pause.setText("Retomar");motion.show("CAM • Caminhada",false);}else if(remaining[0]>0){end[0]=SystemClock.elapsedRealtime()+remaining[0];running[0]=true;pause.setText("Pausar");motion.show("CAM • Caminhada",true);}});
        ui.line(c,"Ritmo confortável. Pare se sentir dor, tontura ou falta de ar incomum. Tempo salvo somente ao confirmar.");
        cardioDialog=new AlertDialog.Builder(a).setTitle("Cardio matinal").setView(c).setNegativeButton("Cancelar",null).setPositiveButton("Salvar tempo realizado",null).create();cardioDialog.show();a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tick=new Runnable(){public void run(){if(cardioDialog==null||!cardioDialog.isShowing())return;if(running[0])remaining[0]=Math.max(0,end[0]-SystemClock.elapsedRealtime());long sec=(remaining[0]+999)/1000;clock.setText(String.format(Locale.US,"%02d:%02d",sec/60,sec%60));if(remaining[0]==0){running[0]=false;pause.setEnabled(false);motion.show("CAM • Caminhada",false);return;}handler.postDelayed(this,200);}};handler.post(tick);
        cardioDialog.getButton(-1).setOnClickListener(v->{if(running[0])remaining[0]=Math.max(0,end[0]-SystemClock.elapsedRealtime());int done=(int)((minutes*60000L-remaining[0])/60000L);p.edit().putInt(key(day,"cardio"),p.getInt(key(day,"cardio"),0)+done).apply();cardioDialog.dismiss();a.render();toast("Caminhada registrada: "+done+" min");});
        cardioDialog.setOnDismissListener(d->{if(tick!=null)handler.removeCallbacks(tick);a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);cardioDialog=null;});
    }
    void body(LinearLayout c,int day){ui.button(c,"Avaliação semanal",()->weekly(day));ui.button(c,"Fotos de evolução • comparação",()->photos(week(day)));ui.button(c,"Registro de exames hormonais",()->labs(day));}
    void weekly(int day){
        int w=week(day),first=(w-1)*7+1,last=Math.min(60,w*7);LinearLayout c=form();ui.line(c,"Semana "+w+" • dias "+first+"–"+last);
        int meals=0,water=0,cardio=0,runs=0,gyms=0,days=0,low=0,sleepCount=0;double sleepSum=0;for(int i=first;i<=last;i++){if(PlanData.date(a,i).isAfter(LocalDate.now()))continue;days++;meals+=PlanData.doneCount(a,i);if(PlanData.water(a,i)>=NutritionData.waterGoal(a,i))water++;cardio+=p.getInt(key(i,"cardio"),0);runs+=NutritionData.workouts(a,PlanData.date(a,i),"run");gyms+=NutritionData.workouts(a,PlanData.date(a,i),"gym");if(p.contains(key(i,"recovery"))&&p.getInt(key(i,"recovery"),3)<3)low++;if(p.contains(key(i,"sleep")))try{sleepSum+=Double.parseDouble(p.getString(key(i,"sleep"),""));sleepCount++;}catch(Exception ignored){}}
        ui.line(c,"Refeições confirmadas: "+meals+"/"+(days*4)+" • hidratação atingida: "+water+"/"+days+" dias");
        ui.line(c,"Caminhada: "+cardio+" min • corrida: "+runs+" • força: "+gyms+" sessões");
        if(sleepCount>0)ui.line(c,"Sono médio: "+String.format(Locale.US,"%.1f",sleepSum/sleepCount)+" h ("+sleepCount+" registros)");
        EditText weight=ui.field(c,"Peso da avaliação (kg)",p.getString(weekKey(w,"weight"),""),true),waist=ui.field(c,"Cintura (cm)",p.getString(weekKey(w,"waist"),""),true);
        if(w>1){String prev=p.getString(weekKey(w-1,"weight"),"");String curr=p.getString(weekKey(w,"weight"),"");if(!prev.isEmpty()&&!curr.isEmpty()){double diff=Double.parseDouble(curr)-Double.parseDouble(prev);ui.line(c,"Variação semanal: "+String.format(Locale.US,"%+.1f",diff)+" kg. Peso não distingue gordura, água e músculo.");}}
        ui.line(c,low>0?"Recuperação baixa registrada: priorize descanso e não reduza calorias.":"Mantenha a meta se estiver evoluindo e recuperando bem. Sem tendência clara por pelo menos 2 semanas, revise registros e procure orientação antes de reduzir calorias.");
        ui.line(c,"Não são feitos cortes automáticos. Compare medidas e força, além do peso.");
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Revisão semanal").setView(scroll(c)).setNegativeButton("Cancelar",null).setPositiveButton("Salvar avaliação",null).create();d.show();d.getButton(-1).setOnClickListener(v->{try{double x=ui.val(weight),y=ui.val(waist);if(!Double.isFinite(x+y)||x<30||x>350||y<30||y>300)throw new Exception();p.edit().putString(weekKey(w,"weight"),String.valueOf(x)).putString(weekKey(w,"waist"),String.valueOf(y)).apply();PlanData.setWeight(a,(float)x);p.edit().putFloat("fit_m0",(float)y).apply();String key="fit_measure_"+LocalDate.now();String old=p.getString(key,"");if(old.isEmpty())p.edit().putString(key,PlanData.fmt((float)x)+" kg • Cintura: "+y+" cm").apply();ReminderScheduler.refreshWater(a,PlanData.today(a));d.dismiss();a.render();}catch(Exception e){toast("Informe peso e cintura válidos.");}});
    }
    void labs(int day){
        LinearLayout c=form();ui.line(c,"Registre resultados e unidades do laboratório. Não altera medicamentos nem interpreta exames.");
        String[] labels={"Testosterona total","Testosterona livre","Estradiol","LH","FSH","Observações / data do exame"};
        EditText[] fields=new EditText[labels.length];for(int i=0;i<labels.length;i++){ui.line(c,labels[i]);fields[i]=new EditText(a);fields[i].setText(p.getString(key(day,"lab"+i),""));fields[i].setHint("Valor e unidade");c.addView(fields[i]);}
        ui.button(c,"Histórico de exames",()->{StringBuilder s=new StringBuilder();for(int i=60;i>=1;i--)if(p.contains(key(i,"lab0"))){s.append(PlanData.date(a,i)).append("\n");for(int j=0;j<labels.length;j++){String val=p.getString(key(i,"lab"+j),"");if(!val.isEmpty())s.append(labels[j]).append(": ").append(val).append("\n");}s.append("\n");}new AlertDialog.Builder(a).setTitle("Histórico").setMessage(s.length()==0?"Sem exames registrados.":s.toString()).setPositiveButton("Fechar",null).show();});
        new AlertDialog.Builder(a).setTitle("Exames e acompanhamento médico").setView(scroll(c)).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{SharedPreferences.Editor ed=p.edit();for(int i=0;i<labels.length;i++)ed.putString(key(day,"lab"+i),fields[i].getText().toString().trim());ed.apply();}).show();
    }
    File photo(int w){return new File(a.getFilesDir(),"n60_"+PlanData.start(a)+"_week"+w+".jpg");}
    void photos(int week){
        LinearLayout c=form();ui.line(c,"Fotos privadas no armazenamento do aplicativo. Compare a primeira semana com a selecionada. Não há upload.");
        String[] labels=new String[9];for(int i=0;i<9;i++)labels[i]="Semana "+(i+1);
        Spinner which=ui.spinner(c,labels,week-1);LinearLayout pair=new LinearLayout(a);c.addView(pair);Runnable paint=()->{pair.removeAllViews();for(int w:new int[]{1,which.getSelectedItemPosition()+1}){LinearLayout col=ui.vertical();pair.addView(col,new LinearLayout.LayoutParams(0,-2,1));ui.line(col,"Semana "+w);ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.FIT_CENTER);col.addView(image,new LinearLayout.LayoutParams(-1,dp(210)));if(photo(w).exists())image.setImageBitmap(BitmapFactory.decodeFile(photo(w).getPath()));else ui.line(col,"Sem foto");}};
        which.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){paint.run();}public void onNothingSelected(AdapterView<?> p){}});
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Fotos de evolução").setView(scroll(c)).setPositiveButton("Fechar",null).create();
        ui.button(c,"Selecionar foto da semana",()->{photoWeek=which.getSelectedItemPosition()+1;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);try{a.startActivityForResult(i,PHOTO_REQUEST);d.dismiss();}catch(Exception ex){toast("Não foi possível abrir fotos.");}});
        ui.button(c,"Excluir foto selecionada",()->{int w=which.getSelectedItemPosition()+1;new AlertDialog.Builder(a).setTitle("Excluir foto da semana "+w+"?").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(x,y)->{photo(w).delete();paint.run();}).show();});
        d.show();paint.run();
    }
    void photoResult(int request,int result,Intent data){if(request!=PHOTO_REQUEST||result!=Activity.RESULT_OK||data==null||data.getData()==null)return;try{android.net.Uri uri=data.getData();BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;try(InputStream in=a.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,opt);}opt.inSampleSize=1;while(Math.max(opt.outWidth,opt.outHeight)/opt.inSampleSize>1200)opt.inSampleSize*=2;opt.inJustDecodeBounds=false;Bitmap b;try(InputStream in=a.getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(in,null,opt);}if(b==null)throw new Exception();try(FileOutputStream out=new FileOutputStream(photo(photoWeek))){if(!b.compress(Bitmap.CompressFormat.JPEG,90,out))throw new Exception();}b.recycle();toast("Foto armazenada localmente.");photos(photoWeek);}catch(Exception e){toast("Não foi possível importar a foto.");}}
    void destroy(){handler.removeCallbacksAndMessages(null);if(cardioDialog!=null)cardioDialog.dismiss();}
}