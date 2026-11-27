package com.mmd.dieta;
import java.util.ArrayList;
import java.util.List;

final class FitnessData {
    static final class Step {
        final String name; final int seconds;
        Step(String n,int s){name=n;seconds=s;}
    }
    static void add(List<Step> a,String n,int m){a.add(new Step(n,m*60));}
    static void cycles(List<Step> a,int n,int walk,int run){
        for(int i=0;i<n;i++){add(a,"CAM • Caminhada moderada",walk);add(a,"RUNL • Trote leve",run);}
    }
    static void minutes(List<Step> a,int total,int walk,int run){
        while(total>0){int w=Math.min(total,walk);add(a,"CAM • Caminhada moderada",w);total-=w;if(total>0){int v=Math.min(total,run);add(a,"RUNL • Trote leve",v);total-=v;}}
    }
    static List<Step> run(int week,int day){
        List<Step> a=new ArrayList<>();add(a,"AQ • Caminhada leve",10);
        switch(week){
            case 1: if(day==0)minutes(a,20,1,1);else if(day==1)cycles(a,7,1,2);else cycles(a,6,2,3);break;
            case 2: if(day==0){cycles(a,3,1,1);cycles(a,3,2,2);}else if(day==1)cycles(a,7,1,2);else cycles(a,6,2,5);break;
            case 3: if(day==0){cycles(a,3,1,1);cycles(a,3,2,2);}else if(day==1)cycles(a,5,2,2);else cycles(a,3,2,8);break;
            case 4: if(day==0)minutes(a,21,1,2);else if(day==1)cycles(a,5,2,3);else a.add(new Step("Desafio 2 km • concluir manualmente",0));break;
            case 5: if(day==0)minutes(a,30,1,3);else if(day==1)minutes(a,30,2,3);else cycles(a,3,2,8);break;
            case 6: if(day==0){cycles(a,3,2,1);cycles(a,3,2,3);}else if(day==1)minutes(a,30,2,3);else cycles(a,3,2,10);break;
            case 7: if(day==0){cycles(a,3,2,1);cycles(a,3,2,4);}else if(day==1)minutes(a,30,2,4);else cycles(a,3,2,10);break;
            case 8: if(day==0){cycles(a,4,2,1);cycles(a,4,1,3);}else if(day==1)minutes(a,30,2,4);else a.add(new Step("Desafio 3 km • concluir manualmente",0));break;
            case 9: if(day==0)cycles(a,3,2,8);else if(day==1)cycles(a,2,3,12);else cycles(a,2,3,12);break;
            case 10: if(day==0)cycles(a,2,3,12);else if(day==1)cycles(a,2,3,13);else cycles(a,2,3,15);break;
            case 11: if(day==0)cycles(a,2,3,12);else if(day==1){add(a,"RUNL • Trote leve",18);add(a,"CAM • Caminhada moderada",3);add(a,"RUNL • Trote leve",12);}else{add(a,"RUNL • Trote leve",20);add(a,"CAM • Caminhada moderada",3);add(a,"RUNL • Trote leve",12);}break;
            case 12: if(day==0)cycles(a,2,3,12);else if(day==1)add(a,"RUNL • Trote leve",25);else a.add(new Step("Desafio até 5 km • concluir manualmente",0));break;
            default: throw new IllegalArgumentException();
        }
        add(a,"ST • Caminhada leve",week==3&&day==0?10:week==3&&day==2?4:5);return a;
    }
    static final class Exercise{
        final String name,tech;final int sets,kind;final int[] rest;final String[] reps,tempo;
        Exercise(String n,String t,int s,int k,int[] d,String[] r,String[] p){name=n;tech=t;sets=s;kind=k;rest=d;reps=r;tempo=p;}
        String rep(int phase,int set){String r=reps[phase];if(r.contains(" "))return r.split(" ")[Math.min(set,r.split(" ").length-1)];return r;}
    }
    static Exercise e(String n,String t,int s,int k,int... rest){
        String[] r=k==1?new String[]{"15 12 12 10","12 10 10 8","12 10 8 6"}:k==2?new String[]{"10+10","12+12","15+15"}:k==3?new String[]{"10+25","12+20","15+15"}:k==4?new String[]{"20+20","15+15","10+10"}:k==5?new String[]{"12+MAX","10+MAX","8+MAX+MAX"}:k==6?new String[]{"25","20","15"}:new String[]{"10–12","8–10","6–8"};
        return new Exercise(n,t,s,k,rest,r,k>=2&&k<=4?new String[]{"1:02","1:02","1:02"}:new String[]{"1:01","1:02","1:03"});
    }
    static Exercise m(String n,int s){return e(n,"Séries múltiplas",s,0,60,75,90);}
    static Exercise p(String n){return e(n,"Pirâmide",4,1,60,75,90);}
    static final Exercise[][] GYM={
        {e("Abdominal com peso","Bi-set",4,3,60,60,60),p("Crucifixo 45°"),m("Supino reto",4),m("Supino 45° halteres",3),m("Cross over",3),e("Rosca direta barra","Séries múltiplas",4,1,60,75,90),m("Rosca Scott",3),m("Rosca direta cabo",3)},
        {e("Abdominal com peso","Bi-set",4,2,60,60,60),p("Desenvolvimento halteres"),m("Abdução unilateral cabo",4),m("Abdução ombros",4),m("Crucifixo invertido",4),p("Encolhimento ombros barra"),m("Remada alta",4),new Exercise("Elevação de tronco • bom dia","Séries múltiplas",4,0,new int[]{60,75,90},new String[]{"10–12","8–10","6–8"},new String[]{"1:01","1:01","1:03"})},
        {e("Abdominais","Bi-set",4,2,60,60,60),p("Puxador frontal fechado supinado"),m("Remada baixa aberta supinado",4),m("Puxador frontal aberta",3),m("Remada unilateral",3),e("Tríceps puxador","Séries múltiplas",4,1,60,75,90),m("Tríceps francês cross polia baixa",3),m("Tríceps testa",3)},
        {e("Banco adutor + banco abdutor","Super set",1,4,30,30,30),e("Mesa flexora","Pirâmide",4,1,45,60,75),e("Banco flexor","Séries múltiplas",3,0,45,60,75),e("Leg 45° pés afastados","Pirâmide",4,1,45,60,75),e("Hack pés juntos","Séries múltiplas",3,0,45,60,75),e("Banco extensor","Drop set",3,5,45,60,75),e("Panturrilha sentado","Séries múltiplas",3,6,30,45,60),e("Panturrilha em pé","Séries múltiplas",3,6,30,45,60)}
    };
}