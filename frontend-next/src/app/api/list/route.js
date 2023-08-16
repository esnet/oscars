import sample from './sample.json' assert { type: 'json' };
import { NextResponse } from 'next/server'

export async function GET(request) {
  return NextResponse.json( sample)
}
