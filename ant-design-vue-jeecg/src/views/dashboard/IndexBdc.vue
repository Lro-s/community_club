<template>
  <div ref="pie" style="width: 450px;height: 450px;margin: auto;margin-top:30px"/>
</template>


<script>
import {findtaginfo} from "@api/api.js";

export default {
  data() {
    return {
      // 定义图表，各种参数
      charts: '',
      datay: ['男','女'],
      optionData:[
        {value:335,name:'男'},
        {value:310,name:'女'}
      ]
    };
  },
  created(){
    findtaginfo().then((res) => {
      console.log(res.result)
      if (res.result != null) {
        this.datay.length = 0;
        this.optionData.length = 0;
        for (let i = 0; i < res.result.length; i++) {
          this.datay.push(res.result[i].tag)
          let a = {value:res.result[i].shuliang,name:res.result[i].tag}
          this.optionData.push(a)
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
      let myEcharts = this.$echarts.init(this.$refs.pie)
      let option = {
        title: {
          text: '博客标签分布数',
          left:'center'
        },
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          x: 'left',
          data:this.datay
        },
        series: [
          {
            name:'标签',
            type:'pie',
            radius:['50%','70%'],
            avoidLabelOverlap: false,
            label: {
              normal: {
                show: false,
                position: 'center'
              },
              emphasis: {
                show: true,
                fontSize: 28,
                fontWeight: 'normal'
              }
            },
            labelLine: {
              normal: {
                show: false
              }
            },
            data:this.optionData
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