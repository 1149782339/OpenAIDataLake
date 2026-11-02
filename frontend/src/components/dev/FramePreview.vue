<script setup lang="ts">
/**
 * 监控画面示意图（内联 SVG 生成，不依赖任何外部图片资源）。
 * 同一个组件用于左侧缩略图与右侧大图，通过 seed 让不同帧呈现不同画面。
 */
import { computed, useId } from 'vue'

interface SceneBlob {
  cx: number
  cy: number
  rx: number
  ry: number
  o: number
}

interface SceneRect {
  x: number
  y: number
  w: number
  h: number
}

interface Scene {
  skyTop: string
  skyBottom: string
  horizon: number
  ground: string
  groundEdge: string
  buildings: SceneRect[]
  lamps: { x: number; y: number }[]
  smoke: SceneBlob[]
  fire: SceneBlob[]
}

const props = withDefaults(
  defineProps<{
    /** 随机种子，不同 seed 生成的画面不同 */
    seed?: number
    /** 宽（px） */
    width?: number
    /** 高（px） */
    height?: number
    /** 是否绘制检测框 */
    showBbox?: boolean
    /** 检测框 [x, y, w, h]，相对坐标 0~1 */
    bbox?: readonly number[] | null
    /** 检测框左上角标签文案 */
    bboxLabel?: string
    /** 左下角水印文案 */
    caption?: string
  }>(),
  {
    seed: 1,
    width: 96,
    height: 54,
    showBbox: false,
    bbox: null,
    bboxLabel: '',
    caption: '',
  },
)

const uid = useId()
const skyId = `fp-sky-${uid}`
const groundId = `fp-ground-${uid}`
const fireId = `fp-fire-${uid}`
const vignetteId = `fp-vig-${uid}`
const smokeId = `fp-smoke-${uid}`

const PALETTES: { skyTop: string; skyBottom: string; ground: string; groundEdge: string }[] = [
  { skyTop: '#1b2438', skyBottom: '#4c5c78', ground: '#2a2f38', groundEdge: '#3b4250' },
  { skyTop: '#33415c', skyBottom: '#8a95a8', ground: '#33383f', groundEdge: '#464d57' },
  { skyTop: '#4c5a74', skyBottom: '#b9c0c9', ground: '#454a52', groundEdge: '#5a606a' },
]

/** 线性同余伪随机，保证同一 seed 结果稳定 */
function makeRnd(seed: number): () => number {
  let s = (Math.abs(Math.floor(seed)) % 99991) + 1
  return () => {
    s = (s * 1664525 + 1013904223) % 4294967296
    return s / 4294967296
  }
}

function buildScene(seed: number, anchor: { x: number; y: number } | null): Scene {
  const rnd = makeRnd(seed)
  const palette = PALETTES[Math.floor(rnd() * PALETTES.length)] ?? PALETTES[0]!
  const horizon = 94 + rnd() * 12

  const buildings: SceneRect[] = []
  let bx = -8
  const bc = 3 + Math.floor(rnd() * 4)
  for (let i = 0; i < bc; i += 1) {
    const w = 20 + rnd() * 36
    const h = 12 + rnd() * 42
    buildings.push({ x: bx, y: horizon - h, w, h })
    bx += w + 2 + rnd() * 10
  }

  const lamps: { x: number; y: number }[] = []
  const lc = 1 + Math.floor(rnd() * 3)
  for (let i = 0; i < lc; i += 1) {
    lamps.push({ x: 14 + rnd() * 292, y: horizon - 6 - rnd() * 34 })
  }

  const fireX = anchor ? anchor.x : 55 + rnd() * 210
  const fireY = anchor ? Math.max(anchor.y, horizon - 4) : horizon - 4 - rnd() * 14

  const fire: SceneBlob[] = []
  const fn = 1 + Math.floor(rnd() * 2)
  for (let i = 0; i < fn; i += 1) {
    fire.push({
      cx: fireX + (rnd() - 0.5) * 16,
      cy: fireY + rnd() * 7,
      rx: 7 + rnd() * 9,
      ry: 5 + rnd() * 8,
      o: 1,
    })
  }

  const smoke: SceneBlob[] = []
  const sn = 4 + Math.floor(rnd() * 4)
  for (let i = 0; i < sn; i += 1) {
    const t = i / sn
    smoke.push({
      cx: fireX + (rnd() - 0.5) * 24 + t * 14,
      cy: fireY - 6 - t * 48 - rnd() * 8,
      rx: 9 + rnd() * 15 + t * 11,
      ry: 6 + rnd() * 9 + t * 5,
      o: 0.28 + rnd() * 0.28,
    })
  }

  return {
    skyTop: palette.skyTop,
    skyBottom: palette.skyBottom,
    horizon,
    ground: palette.ground,
    groundEdge: palette.groundEdge,
    buildings,
    lamps,
    smoke,
    fire,
  }
}

/** 火点锚点：若给了检测框，则火苗画在框内（框底部中心） */
const anchor = computed(() => {
  const b = props.bbox
  if (!b || b.length < 4) return null
  return { x: (b[0] + b[2] / 2) * 320, y: (b[1] + b[3]) * 180 }
})

const scene = computed(() => buildScene(props.seed, anchor.value))

const box = computed(() => {
  const b = props.bbox
  if (!props.showBbox || !b || b.length < 4) return null
  return {
    x: b[0] * 320,
    y: b[1] * 180,
    w: b[2] * 320,
    h: b[3] * 180,
  }
})

/** 检测框四角加强线 */
const corners = computed(() => {
  const b = box.value
  if (!b) return []
  const len = Math.min(12, b.w / 4, b.h / 4)
  return [
    `M${b.x} ${b.y + len}V${b.y}H${b.x + len}`,
    `M${b.x + b.w - len} ${b.y}H${b.x + b.w}V${b.y + len}`,
    `M${b.x} ${b.y + b.h - len}V${b.y + b.h}H${b.x + len}`,
    `M${b.x + b.w - len} ${b.y + b.h}H${b.x + b.w}V${b.y + b.h - len}`,
  ]
})
</script>

<template>
  <svg
    class="fp-svg"
    :width="width"
    :height="height"
    viewBox="0 0 320 180"
    preserveAspectRatio="xMidYMid slice"
    role="img"
    aria-label="监控画面示意图"
  >
    <defs>
      <linearGradient :id="skyId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="scene.skyTop" />
        <stop offset="100%" :stop-color="scene.skyBottom" />
      </linearGradient>
      <linearGradient :id="groundId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="scene.groundEdge" />
        <stop offset="100%" :stop-color="scene.ground" />
      </linearGradient>
      <radialGradient :id="fireId" cx="50%" cy="60%" r="60%">
        <stop offset="0%" stop-color="#ffe9a8" />
        <stop offset="45%" stop-color="#ff7d00" />
        <stop offset="100%" stop-color="#c72c0b" />
      </radialGradient>
      <radialGradient :id="vignetteId" cx="50%" cy="48%" r="72%">
        <stop offset="58%" stop-color="#000000" stop-opacity="0" />
        <stop offset="100%" stop-color="#000000" stop-opacity="0.45" />
      </radialGradient>
      <filter :id="smokeId" x="-30%" y="-30%" width="160%" height="160%">
        <feGaussianBlur stdDeviation="3.2" />
      </filter>
    </defs>

    <!-- 天空 -->
    <rect x="0" y="0" width="320" height="180" :fill="`url(#${skyId})`" />

    <!-- 远景建筑 -->
    <g opacity="0.55">
      <rect
        v-for="(b, i) in scene.buildings"
        :key="`b${i}`"
        :x="b.x"
        :y="b.y"
        :width="b.w"
        :height="b.h + 8"
        fill="#0f1620"
      />
    </g>

    <!-- 路灯 -->
    <g v-for="(l, i) in scene.lamps" :key="`l${i}`">
      <rect :x="l.x" :y="l.y" width="1.4" :height="scene.horizon - l.y + 4" fill="#0f1620" opacity="0.6" />
      <circle :cx="l.x + 0.7" :cy="l.y" r="2.2" fill="#ffe9a8" opacity="0.85" />
    </g>

    <!-- 地面 -->
    <rect x="0" :y="scene.horizon" width="320" :height="180 - scene.horizon" :fill="`url(#${groundId})`" />
    <rect x="0" :y="scene.horizon" width="320" height="1" fill="#9aa4b2" opacity="0.45" />

    <!-- 烟雾 -->
    <g :filter="`url(#${smokeId})`">
      <ellipse
        v-for="(s, i) in scene.smoke"
        :key="`s${i}`"
        :cx="s.cx"
        :cy="s.cy"
        :rx="s.rx"
        :ry="s.ry"
        fill="#d9dde3"
        :opacity="s.o"
      />
    </g>

    <!-- 火光 -->
    <g>
      <ellipse
        v-for="(f, i) in scene.fire"
        :key="`f${i}`"
        :cx="f.cx"
        :cy="f.cy"
        :rx="f.rx"
        :ry="f.ry"
        :fill="`url(#${fireId})`"
        :opacity="f.o"
      />
      <ellipse
        v-for="(f, i) in scene.fire"
        :key="`fg${i}`"
        :cx="f.cx"
        :cy="f.cy - 2"
        :rx="f.rx * 0.55"
        :ry="f.ry * 0.55"
        fill="#fff0c2"
        opacity="0.8"
      />
    </g>

    <!-- 暗角 -->
    <rect x="0" y="0" width="320" height="180" :fill="`url(#${vignetteId})`" />

    <!-- 检测框 -->
    <g v-if="box">
      <rect
        :x="box.x"
        :y="box.y"
        :width="box.w"
        :height="box.h"
        fill="#f53f3f"
        fill-opacity="0.08"
        stroke="#f53f3f"
        stroke-width="2"
      />
      <path
        v-for="(c, i) in corners"
        :key="`c${i}`"
        :d="c"
        fill="none"
        stroke="#f53f3f"
        stroke-width="3.4"
        stroke-linecap="square"
      />
      <g v-if="bboxLabel">
        <rect :x="box.x" :y="box.y - 17" :width="bboxLabel.length * 9 + 10" height="16" rx="2" fill="#f53f3f" />
        <text :x="box.x + 5" :y="box.y - 5" fill="#ffffff" font-size="11">{{ bboxLabel }}</text>
      </g>
    </g>

    <!-- 水印 -->
    <text v-if="caption" x="8" y="172" fill="#ffffff" fill-opacity="0.72" font-size="9">
      {{ caption }}
    </text>
  </svg>
</template>

<style scoped>
.fp-svg {
  display: block;
  background: #1b2438;
}
</style>
