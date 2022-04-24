<template>
  <div ref="bar" style="width: 450px;height: 450px;margin: auto;margin-top:30px"/>
</template>


<script>
import {findcommunityinfo} from "@api/api.js";

export default {
  data() {
    return {
      // 定义图表，各种参数
      msg: "柱状图",
      datay: ["篮球","足球","羽毛球","排球"],
      datay1: [1,2,3,4]
    };
  },
  created(){
    findcommunityinfo().then((res) => {
      console.log(res.result)
      if (res.result != null) {
        this.datay.length = 0;
        this.datay1.length = 0;
        for (let i = 0; i < res.result.length; i++) {
          this.datay.push(res.result[i].communityname);
          console.log(this.datay)
          this.datay1.push(res.result[i].communitytotal);
          console.log(this.datay1)
        }
      }
    });
  },
  watch: {
    datay: {
      //对于深层对象的属性，watch不可达，因此对数组监控需要将数组先清空，再添加数据
      handler: function () {
        this.initEcharts();
      },
      deep: true,
    },
  },
  methods: {
    initEcharts() {
      let myEcharts = this.$echarts.init(this.$refs.bar)
      let option = {
        title: {
          text: '社团人数分布情况',
          left:'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: this.datay
        },
        yAxis: {
          type: 'value',
          name:"人数",
          min:0,
          max:20
        },
        legend: {

        },
        series: [
          {
            name: this.datay,
            type: 'bar',
            data: this.datay1
          }
        ]
      }
      myEcharts.setOption(option)
    }

  },
  //在mouted钩子中调用
  mounted() {
    this.initEcharts()
  }
}
</script>