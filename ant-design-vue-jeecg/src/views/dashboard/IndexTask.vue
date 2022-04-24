<template>
  <div ref="line" style="width: 450px;height: 450px;margin: auto;margin-top:30px"/>
</template>


<script>
import {findgradeinfo} from "@api/api.js";


export default {
  data() {
    return {
      // 定义图表，各种参数
      charts: '',
      datay: ['大一','大二','大三','大四'],
      datay1: [10,20,5,7],
      datay2: [5,7,8,10]
    };
  },
  created(){
    findgradeinfo().then((res) => {
      console.log(res.result)
      if (res.result != null) {
        this.datay1.length = 0;
        this.datay2.length = 0;
        for (let i = 0; i < res.result.length; i++) {
          if(res.result[i].sex=='1')
            this.datay1.push(res.result[i].shuliang)
          else if(res.result[i].sex=='2')
            this.datay2.push(res.result[i].shuliang)
        }
      }
    });
  },
  watch: {
    datay1: {
      //对于深层对象的属性，watch不可达，因此对数组监控需要将数组先清空，再添加数据
      handler: function () {
        this.initEcharts();
      },
      deep: true,
    },
  },
  methods: {
    initEcharts() {
      let myEcharts = this.$echarts.init(this.$refs.line)
      let option = {
        title: {
          text: '社团学生年级分布情况',
          left:'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          data: this.datay
        },
        yAxis: {
          type:'value',
          minInterval: 1
        },
        series: [
          {
            name:'男生人数',
            data: this.datay1,
            type: 'line',
            label: {
              show: false,
              position: 'bottom',
              textStyle: {
                fontSize: 20
              },
              emphasis: {
                show: true
              }
            }
          },
          {
            name:'女生人数',
            data: this.datay2,
            type: 'line',
            label: {
              show: false,
              position: 'bottom',
              textStyle: {
                fontSize: 20
              },
              emphasis: {
                show: true
              }
            }
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