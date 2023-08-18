import sample from './sample.json' assert { type: 'json' };
import { NextResponse } from 'next/server'
import {getBackendServerUrl} from "../../../lib/clients";

export async function GET(request) {
  //
  if (false) {
    return NextResponse.json(sample)
  }
  // using the request param disables server side caching
  const { searchParams } = new URL(request.url)

  const apiEndpoint = getBackendServerUrl()+'/api/conn/simplelist'

  const res = await fetch(apiEndpoint)

  if (!res.ok) {
    // This will activate the closest `error.js` Error Boundary
    throw new Error('Failed to fetch data')
  }
  const list = await res.json()

  return NextResponse.json(list)



}
