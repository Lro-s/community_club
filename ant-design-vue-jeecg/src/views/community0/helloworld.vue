<template>
  <div :style="background" class="background">
    <div v-if="msg1!=''" class="h">
      <span class="a">亲爱的{{user}}您好,您已经加入{{msg1}}了</span>
    </div>
    <div v-else-if="msg==false " class="h">
      <span class="a">亲爱的{{user}}您好,您还没有申请加入社团,这边给您推荐一些热门社团：</span>
      <br>
      <ol class="cn">
        <li v-for="i in community1" class="b">
          {{ i }}
        </li>
      </ol>
    </div>
    <div v-else-if="msg==true" class="h">
      <span class="a1"> 亲爱的{{user}}你好,<br>这边您申请了:</span>
      <ol class="cn1">
        <span v-for="i in community" class="b">
          {{ i }}
        </span>
      </ol>
      <br>
      <span class="c">这边发现用户<span v-for="i in user1" class="d">{{i}}</span>和您相似，推荐相似社团给您<span v-for="i in community2" class="d">{{i}}</span></span>
    </div>
  </div>
</template>

<script>
import {getAction, postAction,fin} from '@/api/manage'
import {JeecgListMixin} from "@/mixins/JeecgListMixin";
import JDate from '@/components/jeecg/JDate'
import store from '@/store'
import {findusernameinapplication} from "@api/api";

export default {
  data() {
    return {
      msg: "",
      msg1:"",
      user:store.getters.userInfo.username,
      user1:[],
      community: [],
      community1:[],
      community2:[],
      background:{
        // 背景图片地址
        backgroundImage: 'url(' + require('../../assets/bg.jpeg') + ')',
        // 背景图片是否重复
        backgroundRepeat: 'no-repeat',
        // 背景图片大小
        backgroundSize: 'cover',
        // 背景颜色
        backgroundColor: '#000',
        // 背景图片位置
        backgroundPosition: 'center top'
      }
    }
  },
  methods: {
    panduan() {
      let usercode = store.getters.userInfo.username;
      // console.log(usercode)
      let formdata = new FormData()
      formdata.append('usercode',usercode)
      postAction("/community/findcommunitynamefromsysuser",formdata).then((res) => {
        console.log(res)
        this.msg1 = res;
      });
      postAction("/community/findusernameinapplication",formdata).then((res) => {
        console.log(res)
        if (res.success == true) {
          this.community.length=0
          this.msg = res.success;
          for (let i = 0; i < res.result.length; i++) {
            this.community.push(res.result[i].communityname)
          }
        }
      });
      getAction("/community/topcommunityname").then((res) => {
        if (res.result != null) {
          this.community1.length=0
          console.log(res)
          for (let i = 0; i < res.result.length; i++) {
            this.community1.push(res.result[i].communityname)
          }
        }
      });
      getAction("/python/chuanzhi").then((res) => {
        var a = JSON.parse(res[0])
        let usercode = store.getters.userInfo.username;
        console.log(usercode)
        // var b = JSON.parse(res[1])
        console.log(res[0])
        console.log(res[1])
        for (let c = 0; c < a[usercode].length; c++) {
          this.user1.push(a[usercode][c])
        }
        var b = JSON.parse(res[1])
        for (let c = 0; c< b[usercode].length;c++){
          this.community2.push(b[usercode][c])
      }
      });
    }
  },
  created() {
    this.panduan();
  }
}
</script>
<style>
.h {
  width: 400px;
  height: 200px;
  background: #87e8de;
  border: 1px solid #cccccc;
  position: fixed;
  left: 50%;
  top: 50%;
}
.d{
  color: red;
  margin: 5px;
}

.background {
  width:100%;
  height:100%;
  position:absolute;
  background-size:100% 100%;
}

.a{
  font-size: 18px;
}

.a1{
  font-size: 17px;
}

.b{
  font-size: 16px;
  margin: 8px;
}

.c{
  font-size: 17px;

}

.cn{
  position: fixed;
  left: 60%;
  top: 52%;
  color: red;
  margin: 10px;
}

.cn1{
  position: fixed;
  left: 55%;
  top: 52.5%;
  color: red;
  margin: 10px;
}
</style>